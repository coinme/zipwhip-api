package com.zipwhip.gson;

import com.google.gson.*;
import com.zipwhip.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Date: 8/22/13
 * Time: 5:08 PM
 *
 * @author Michael
 * @version 1
 */
public class GsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonUtil.class);

    public static Integer getInt(JsonElement element) {
        if (isNull(element)) {
            return null;
        }

        if (element instanceof JsonPrimitive) {
            return element.getAsInt();
        }

        return null;
    }

    public static Long getLong(JsonElement element) {
        if (isNull(element)) {
            return null;
        }

        if (element instanceof JsonPrimitive) {
            return element.getAsLong();
        }

        return null;
    }

    public static String getString(JsonElement element) {
        if (isNull(element)) {
            return null;
        }

        if (element instanceof JsonPrimitive) {
            return element.getAsString();
        }

        return element.toString();
    }

    public static String getString(JsonElement element, String field) {
        if (element instanceof JsonObject) {
            return getString(((JsonObject) element).get(field));
        }

        throw new JsonParseException("Not found: " + field);
    }

    public static Object getDefaultValue(ClassLoader classLoader, JsonDeserializationContext context, JsonElement element) {
        if (GsonUtil.isNull(element)) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            return GsonUtil.getPrimitiveValue((JsonPrimitive) element);
        }

        if (element instanceof JsonObject) {
            JsonObject object = (JsonObject)element;

            String clazzName = GsonUtil.getString(object, "class");

            if (StringUtil.isNullOrEmpty(clazzName)) {
                // No class name. cannot deserialize.
                return context.deserialize(element, Map.class);
            }

            try {
                Class<?> clazz = classLoader.loadClass(clazzName);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Going to custom parse json for class: " + clazz);
                }

                return context.deserialize(element, clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Failed to find class for " + clazzName + ". Going to instead just return a json string.");

                return context.deserialize(element, Map.class);
            }
        }

        if (element instanceof JsonArray) {
            JsonArray array = (JsonArray) element;

            if (array.size() <= 0) {
                // Empty array same thing as null?
                return null;
            }

            Collection<Object> collection = new LinkedList<Object>();

            for (JsonElement jsonElement : array) {
                collection.add(getDefaultValue(classLoader, context, jsonElement));
            }

            return collection;
        }

        return element.getAsJsonObject();
    }

    public static Set<String> getSet(JsonElement element) {
        Set<String> result = new TreeSet<String>();

        JsonArray array = (JsonArray) element;

        for (JsonElement jsonElement : array) {
            String value = getString(jsonElement);
            if (StringUtil.isNullOrEmpty(value)) {
                continue;
            }

            result.add(value);
        }

        return result;
    }

    public static boolean isNull(JsonElement e) {
        return e == null || e.isJsonNull();
    }

    public static Serializable getPrimitiveValue(JsonPrimitive primitive) {
        if (isNull(primitive)) {
            return null;
        }

        if (primitive.isString()) {
            return primitive.getAsString();
        } else if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            return primitive.getAsNumber();
        }

        throw new JsonParseException("Not primitive?");
    }


}
