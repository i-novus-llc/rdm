package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UpdateValue<T> {

    private final T value;

    /**
     * Constructs an empty instance.
     *
     */
    private UpdateValue() {
        this.value = null;
    }

    /**
     * Constructs an instance with the value present.
     *
     * @param value the value to be present
     */
    private UpdateValue(T value) {
        this.value = value;
    }

    /**
     * Returns an {@code UpdateValue} with the specified present value.
     *
     * @param <T> the class of the value
     * @param value the value to be present, which must be non-null
     * @return an {@code UpdateValue} with the value present
     */
    public static <T> UpdateValue<T> of(T value) {
        return new UpdateValue<>(value);
    }

    /**
     * If a value is present in this {@code UpdateValue}, returns the value,
     * otherwise returns null (without throwing an exception.
     *
     * @return the value held by this {@code UpdateValue}
     *
     * @see UpdateValue#isPresent()
     */
    public T get() {
        return value;
    }

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}.
     *
     * @return {@code true} if there is a value present, otherwise {@code false}
     */
    @JsonIgnore
    public boolean isPresent() {
        return value != null;
    }

    public T getValue() {
        return get();
    }
}