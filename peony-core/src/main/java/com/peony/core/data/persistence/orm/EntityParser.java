package com.peony.core.data.persistence.orm;

import java.util.Map;

public interface EntityParser {
    public Map<String,Object> toMap(Object entity);
}
