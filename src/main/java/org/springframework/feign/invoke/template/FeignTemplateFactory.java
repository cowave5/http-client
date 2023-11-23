package org.springframework.feign.invoke.template;

import feign.MethodMetadata;
import feign.Param;
import feign.RequestTemplate;

import java.util.*;

import static feign.Util.checkArgument;
import static feign.Util.checkState;

/**
 *
 * @author shanhuiming
 *
 */
public class FeignTemplateFactory {

    protected final MethodMetadata metadata;
    private final Map<Integer, Param.Expander> indexToExpander = new LinkedHashMap<Integer, Param.Expander>();

    @SuppressWarnings("deprecation")
    public FeignTemplateFactory(MethodMetadata metadata) {
        this.metadata = metadata;
        if (metadata.indexToExpander() != null) {
            indexToExpander.putAll(metadata.indexToExpander());
            return;
        }
        if (metadata.indexToExpanderClass().isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Class<? extends Param.Expander>> indexToExpanderClass : metadata.indexToExpanderClass().entrySet()) {
            try {
                indexToExpander.put(indexToExpanderClass.getKey(), indexToExpanderClass.getValue().newInstance());
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public RequestTemplate create(Object[] argv) {
        RequestTemplate mutable = new RequestTemplate(metadata.template());
        if (metadata.urlIndex() != null) {
            int urlIndex = metadata.urlIndex();
            checkArgument(argv[urlIndex] != null, "URI parameter %s was null", urlIndex);
            mutable.insert(0, String.valueOf(argv[urlIndex]));
        }
        Map<String, Object> varBuilder = new LinkedHashMap<String, Object>();
        for (Map.Entry<Integer, Collection<String>> entry : metadata.indexToName().entrySet()) {
            int i = entry.getKey();
            Object value = argv[entry.getKey()];
            if (value != null) { // Null values are skipped.
                if (indexToExpander.containsKey(i)) {
                    value = expandElements(indexToExpander.get(i), value);
                }
                for (String name : entry.getValue()) {
                    varBuilder.put(name, value);
                }
            }
        }

        RequestTemplate template = resolve(argv, mutable, varBuilder);
        if (metadata.queryMapIndex() != null) {
            // add query map parameters after initial resolve so that they take
            // precedence over any predefined values
            template = addQueryMapQueryParameters(argv, template);
        }

        if (metadata.headerMapIndex() != null) {
            template = addHeaderMapHeaders(argv, template);
        }

        return template;
    }

    @SuppressWarnings("rawtypes")
    private Object expandElements(Param.Expander expander, Object value) {
        if (value instanceof Iterable) {
            return expandIterable(expander, (Iterable) value);
        }
        return expander.expand(value);
    }

    @SuppressWarnings("rawtypes")
    private List<String> expandIterable(Param.Expander expander, Iterable value) {
        List<String> values = new ArrayList<String>();
        for (Object element : (Iterable) value) {
            if (element!=null) {
                values.add(expander.expand(element));
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private RequestTemplate addHeaderMapHeaders(Object[] argv, RequestTemplate mutable) {
        Map<Object, Object> headerMap = (Map<Object, Object>) argv[metadata.headerMapIndex()];
        for (Map.Entry<Object, Object> currEntry : headerMap.entrySet()) {
            checkState(currEntry.getKey().getClass() == String.class, "HeaderMap key must be a String: %s", currEntry.getKey());

            Collection<String> values = new ArrayList<String>();

            Object currValue = currEntry.getValue();
            if (currValue instanceof Iterable<?>) {
                Iterator<?> iter = ((Iterable<?>) currValue).iterator();
                while (iter.hasNext()) {
                    Object nextObject = iter.next();
                    values.add(nextObject == null ? null : nextObject.toString());
                }
            } else {
                values.add(currValue == null ? null : currValue.toString());
            }

            mutable.header((String) currEntry.getKey(), values);
        }
        return mutable;
    }

    @SuppressWarnings("unchecked")
    private RequestTemplate addQueryMapQueryParameters(Object[] argv, RequestTemplate mutable) {
        Map<Object, Object> queryMap = (Map<Object, Object>) argv[metadata.queryMapIndex()];
        for (Map.Entry<Object, Object> currEntry : queryMap.entrySet()) {
            checkState(currEntry.getKey().getClass() == String.class, "QueryMap key must be a String: %s", currEntry.getKey());

            Collection<String> values = new ArrayList<String>();

            Object currValue = currEntry.getValue();
            if (currValue instanceof Iterable<?>) {
                Iterator<?> iter = ((Iterable<?>) currValue).iterator();
                while (iter.hasNext()) {
                    Object nextObject = iter.next();
                    values.add(nextObject == null ? null : nextObject.toString());
                }
            } else {
                values.add(currValue == null ? null : currValue.toString());
            }

            mutable.query(metadata.queryMapEncoded(), (String) currEntry.getKey(), values);
        }
        return mutable;
    }

    protected RequestTemplate resolve(Object[] argv, RequestTemplate mutable,
                                      Map<String, Object> variables) {
        return mutable.resolve(variables);
    }
}
