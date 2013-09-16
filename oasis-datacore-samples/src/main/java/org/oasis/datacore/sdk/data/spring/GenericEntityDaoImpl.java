package org.oasis.datacore.sdk.data.spring;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class GenericEntityDaoImpl<T extends GenericEntity<T>> {

   @Autowired
   private MongoOperations mgo;
   
   private Class<T> objectClass;
   
   @SuppressWarnings("unchecked")
   public GenericEntityDaoImpl() {
       int retriesCount = 0;
       Class<?> clazz = getClass();
       while(!(clazz.getGenericSuperclass() instanceof ParameterizedType) && (retriesCount < 5)) {
           clazz = clazz.getSuperclass();
           retriesCount ++;
       }
       objectClass = (Class<T>) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
   }
   
   protected final Class<T> getObjectClass() {
       return objectClass;
   }
   
   public MongoOperations getMongoOperations() {
      return mgo;
   }
   
   
   
   
   public GenericEntity<?> getEntity(Class<? extends GenericEntity<?>> clazz, String _id) {
       return (GenericEntity<?>) mgo.findById(_id, clazz);
   }
   
   @SuppressWarnings("unchecked")
   public T getById(String _id) {
       return (T) mgo.findById(_id, getObjectClass());
   }
   
   @SuppressWarnings("unchecked")
   public T getByField(String fieldName, Object fieldValue) {
       return mgo.findOne(new Query(new Criteria(fieldName).is(fieldValue)), getObjectClass());
   }
   
   /*public void update(T entity) {
       mgo.save(entity);
   }
   
   public void create(T entity) throws EntityNotNewException {
       if (entity.isNew()) {
           mgo.save(entity);
       } else {
           throw new EntityNotNewException("Entity ID : " + entity.getId());
       }
   }*/
   
   public void save(T entity) {
      mgo.save(entity);
       /*if (entity.isNew()) {
           mgo.save(entity);
       } else {
           mgo.update(entity);
       }*/
   }
   
   public void delete(T entity) {
       mgo.remove(entity);
   }
   
   public T refresh(T entity) {
       return getById(entity.getId());
   }
   
   public List<T> list() {
       return mgo.findAll(getObjectClass());
   }
   
   public List<T> listByField(String fieldName, Object fieldValue) {
      return mgo.find(new Query(new Criteria(fieldName).is(fieldValue)), getObjectClass());
   }
   
   /*public List<T> list(Class<? extends T> objectClass, Criterion filter, Order order, Integer limit, Integer offset) {
       List<T> entities = new LinkedList<T>();
       try {
           Criteria criteria = buildCriteria(objectClass, null, filter, order, limit, offset);
           
           entities = criteria.list();
           
           if(order == null) {
               Collections.sort(entities);
           }
           
           return entities;
       } catch(DataAccessException e) {
           return entities;
       }
   }
   
   public Long count() {
       return count(getObjectClass(), null, null, null, null);
   }
   
   public Long count(Class<? extends T> objectClass, Criterion filter, Order order, Integer limit, Integer offset) {
       Criteria criteria = buildCriteria(objectClass, Projections.rowCount(), filter, order, limit, offset);
       
       Long count = (Long) criteria.uniqueResult();
       
       return count;
   }
   
   protected Criteria buildCriteria(Class<? extends T> objectClass,
           Projection projection, Criterion filter, Order order, Integer limit, Integer offset) {
       Criteria criteria = mgo.createCriteria(objectClass);
       if(projection != null) {
           criteria.setProjection(projection);
       }
       if(filter != null) {
           criteria.add(filter);
       }
       if(limit != null) {
           criteria.setMaxResults(limit);
       }
       if(offset != null) {
           criteria.setFirstResult(offset);
       }
       if(order != null) {
           criteria.addOrder(order);
       }
       return criteria;
   }*/
}
