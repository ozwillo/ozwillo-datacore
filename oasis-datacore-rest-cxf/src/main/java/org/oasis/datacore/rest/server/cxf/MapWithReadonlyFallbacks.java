package org.oasis.datacore.rest.server.cxf;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;


/**
 * Map whose reads are done in ordered fallback maps if not found,
 * i.e. a way to avoid merging maps.
 * Write operations are done in primary map only.
 * Introspection are not implemented since would require more work.
 * Size() is not implemented since it could be wrong unless doing
 * actual merge.
 * 
 * @author mdutoo
 *
 * @param <K>
 * @param <V>
 */
public class MapWithReadonlyFallbacks<K, V> implements Map<K, V> {
   
   private Map<K, V> map;
   private List<Map<K, V>> fallbacks;

   /**
    * 
    * @param map not null
    * @param fallbacks none null
    */
   public MapWithReadonlyFallbacks(Map<K, V> map, List<Map<K, V>> fallbacks) {
      this.map = map;
      this.fallbacks = fallbacks;
   }

   /** @throws NotImplementedException can' be computed without merging
    * since some fallback values might be hidden by map's ones */ 
   @Override
   public int size() {
      throw new NotImplementedException(); // and not much sense anyway
   }

   @Override
   public boolean isEmpty() {
      if (!map.isEmpty()) {
         return false;
      }
      for (Map<K, V> fallback : fallbacks) {
         if (!fallback.isEmpty()) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean containsKey(Object key) {
      if (map.containsKey(key)) {
         return true;
      }
      for (Map<K, V> fallback : fallbacks) {
         if (fallback.containsKey(key)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean containsValue(Object value) {
      if (map.containsValue(value)) {
         return true;
      }
      for (Map<K, V> fallback : fallbacks) {
         if (fallback.containsValue(value)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public V get(Object key) {
      V value = map.get(key);
      if (value != null) {
         return value;
      }
      for (Map<K, V> fallback : fallbacks) {
         value = fallback.get(key);
         if (value != null) {
            return value;
         }
      }
      return null;
   }

   @Override
   public V put(K key, V value) {
      return this.map.put(key, value);
   }

   @Override
   public V remove(Object key) {
      return this.map.remove(key);
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      this.map.putAll(m);
   }

   @Override
   public void clear() {
      this.map.clear();
   }

   /** @throws NotImplementedException would require a Set backed by MapWithFallbacks */
   @Override
   public Set<K> keySet() {
      throw new NotImplementedException();
   }

   /** @throws NotImplementedException would require a collection backed by MapWithFallbacks */
   @Override
   public Collection<V> values() {
      throw new NotImplementedException();
   }

   /** @throws NotImplementedException would require a Set backed by MapWithFallbacks */
   @Override
   public Set<java.util.Map.Entry<K, V>> entrySet() {
      throw new NotImplementedException();
   }

}
