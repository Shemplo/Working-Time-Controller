package ru.shemplo.wtc.structures;

import java.util.Objects;

public class Pair <F, S> {
    
    public final F F;
    public final S S;
    
    public Pair (F F, S S) {
        this.F = F; this.S = S;
    }
    
    @Override
    public String toString () {
        return "<" + F + "; " + S + ">";
    }
    
    @Override
    public boolean equals (Object obj) {
        if (Objects.isNull (obj) 
            || !(obj instanceof Pair)) { 
            return false; 
        }
        
        Pair <?, ?> pair = (Pair <?, ?>) obj;
        return (Objects.isNull (F) ? Objects.isNull (pair.F) : F.equals (pair.F))
                && (Objects.isNull (S) ? Objects.isNull (pair.S) : S.equals (pair.S));
    }
    
    public static <F, S> Pair <F, S> mp (F F, S S) {
        return new Pair <> (F, S);
    }
    
}
