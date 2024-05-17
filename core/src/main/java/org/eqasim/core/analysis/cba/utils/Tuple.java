package org.eqasim.core.analysis.cba.utils;

import java.io.Serializable;

public final class Tuple<A, B> implements Serializable {
    private static final long serialVersionUID = 1L;

    public static <A, B> org.matsim.core.utils.collections.Tuple<A, B> of(final A first, final B second) {
        return new org.matsim.core.utils.collections.Tuple<>(first, second);
    }

    private A first;

    private B second;

    public Tuple(final A first, final B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return this.first;
    }

    public B getSecond() {
        return this.second;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    /**
     * @see Object#equals(Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Tuple)) return false;
        Tuple o = (Tuple) other;
        if (this.first != null && this.second != null && o.first != null && o.second != null) {
            return (this.first.equals(o.first) && this.second.equals(o.second));
        }
        boolean firstEquals = (this.first == null) && (o.first == null);
        boolean secondEquals = (this.second == null) && (o.second == null);
        if (!firstEquals && this.first != null && o.first != null) {
            firstEquals = this.first.equals(o.first);
        }
        if (!secondEquals && this.second != null && o.second != null) {
            secondEquals = this.second.equals(o.second);
        }
        return firstEquals && secondEquals;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (this.first == null ? 0 : this.first.hashCode()) +
                (this.second == null ? 0 : this.second.hashCode());
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        buffer.append("[Tuple: [First: " );
        buffer.append(this.first.toString());
        buffer.append("], [Second: ");
        buffer.append(this.second.toString());
        buffer.append("]]");
        return buffer.toString();
    }

}
