package ru.shemplo.wtc.structures;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CacheLine <K extends Comparable <K>, V> {
    
    /*
     * Here CASN must be implemented for consistency
     * 
     */
    
    private final AtomicReference <Node <K, V>> HEAD = new AtomicReference <> ();
    
    private final Comparator <K> ORDER;
    private final int SIZE_LIMIT;
    
    public CacheLine (int size, Comparator <K> comparator) {
        this.ORDER = comparator;
        this.SIZE_LIMIT = size;
    }
    
    private static class Node <K, V> {
        
        private final AtomicReference <Node <K, V>> 
            NEXT = new AtomicReference <> ();
        
        public final V VALUE; 
        public final K KEY;
        
        public Node (K key, V value) {
            this.VALUE = value;
            this.KEY = key;
        }
        
    }
    
    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder ("[");
        Node <K, V> current = HEAD.get ();
        
        while (!Objects.isNull (current)) {
            sb.append ("<");
            sb.append (current.KEY);
            sb.append ("; ");
            sb.append (current.VALUE);
            sb.append (">");
            
            current = current.NEXT.get ();
        }
        
        sb.append ("]");
        return sb.toString ();
    }
    
    public void insert (K key, V value) {
        if (Objects.isNull (key)) {
            throw new IllegalArgumentException ("key == null");
        }
        
        Node <K, V> node = new Node <> (key, value);
        while (true) {
            // In case when CacheLine is empty
            if (HEAD.compareAndSet (null, node)) { break; }
            
            Node <K, V> current = HEAD.get (), previous = null;
            int carriage = 0;
            
            while (!Objects.isNull (current)) {
                if (ORDER.compare (key, current.KEY) > 0) {
                    break;
                }
                
                previous = current; carriage += 1;
                current = current.NEXT.get ();
            }
            
            // Key of inserting value is too small
            if (carriage >= SIZE_LIMIT) {
                previous.NEXT.set (null);
                break;
            }
            
            if (Objects.isNull (previous)) { 
                // It means that inserting value has the biggest key
                
                node.NEXT.set (current);
                if (HEAD.compareAndSet (current, node)) { 
                    break; 
                }
            } else {
                // It means that new value will be inserted in center
                
                node.NEXT.set (current);
                if (previous.NEXT.compareAndSet (current, node)) {
                    break; 
                }
            }
        }
    }
    
    public void forEach (Consumer <Pair <K, V>> action) {
        if (Objects.isNull (action)) {
            throw new IllegalArgumentException ("action == null");
        }
        
        Node <K, V> current = HEAD.get ();
        int carriage = 0;
        
        while (!Objects.isNull (current) && carriage < SIZE_LIMIT) {
            action.accept (Pair.mp (current.KEY, current.VALUE));
            current = current.NEXT.get ();
            carriage += 1;
        }
        
        if (!Objects.isNull (current)) {
            current.NEXT.set (null);
        }
    }
    
}
