package com.k8scms.cms.utils;

public class ToString {
    ToStringFunction<String> apply;

    public ToString(ToStringFunction<String> apply) {
        this.apply = apply;
    }

    @Override
    public String toString() {
        try {
            return apply.invoke();
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot execute apply", e);
        }
    }

    public interface ToStringFunction<T> {
        T invoke();
    }
}
