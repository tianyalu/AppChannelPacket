package com.sty.app.build_channels_adapt_v2;

public class Pair<A, B> {
    private final A mFirst;
    private final B mSecond;

    public Pair(A mFirst, B mSecond) {
        this.mFirst = mFirst;
        this.mSecond = mSecond;
    }

    public static <A, B> Pair<A, B> of(final A first, final B second) {
        return new Pair<>(first, second);
    }

    public A getmFirst() {
        return mFirst;
    }

    public B getmSecond() {
        return mSecond;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mFirst == null) ? 0 : mFirst.hashCode());
        result = prime * result + ((mSecond == null) ? 0 : mSecond.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Pair other = (Pair) obj;
        if(mFirst == null) {
            if(other.mFirst != null) {
                return false;
            }
        }else if(!mFirst.equals(other.mFirst)) {
            return false;
        }

        if(mSecond == null) {
            if(other.mSecond != null) {
                return false;
            }
        }else if(!mSecond.equals(other.mSecond)) {
            return false;
        }
        return true;
    }
}
