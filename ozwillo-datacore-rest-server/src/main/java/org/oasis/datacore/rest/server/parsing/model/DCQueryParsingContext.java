package org.oasis.datacore.rest.server.parsing.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCI18nField;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class DCQueryParsingContext extends DCResourceParsingContext {
   
   // BEFORE INITBUILDCRITERIA :
   
   private Map<String,List<OperatorValue>> entityFieldPathToOperatorValuesMap
      = new LinkedHashMap<String,List<OperatorValue>>(3); // keep order !
   private String globalLanguage = null;
   private Map<String,String> entityFieldPathToLanguageMap = new HashMap<String,String>(3);
   private Map<String, String> fieldOperatorStorageNameMap = new HashMap<String, String>(3);
   /** allows to know merge order ; null means no fulltext */
   private List<QueryOperatorsEnum> fulltextSorts = null;
   
   // AFTER INITBUILDCRITERIA & IN ENTER/EXIT :
   
   // TODO privilege indexed fields !!!!!!!!!!
   private Criteria criteria; // created in initBuildCriteria()
   private String fulltextRegexSuffix;
   /** set to fieldOperatorStorageName if any */
   private String currentEntityFieldPath;
   /** stays the same */
   private String currentOrigEntityFieldPath;
   private String currentLanguage = null;
   /** one in case of single criteria field query, more in case of ex. between or fulltext */
   private List<Criteria> currentCriterias = null;
   /** HACK to allow merging several Spring Mongo Criteria */
   private static Field criteriaChainField;

   // TODO sort, on INDEXED (Queriable) field
   private Sort sort; // created in initBuildCriteria()
   private int aggregatedQueryLimit = 0;
   private boolean hasNoIndexedField = false;
   private Set<String> forbiddenMixins = new HashSet<String>();
   private Set<String> topLevelAllowedMixins = new HashSet<String>();
   
   static {
      try {
         criteriaChainField = Criteria.class.getDeclaredField("criteriaChain");
         criteriaChainField.setAccessible(true);
      } catch (NoSuchFieldException | SecurityException e) {
         throw new RuntimeException(e);
      }
   }
   
   public DCQueryParsingContext(DCModelBase model, DCModelBase storageModel) {
      super(model, storageModel, null);
   }

   public void addOperatorValue(String entityFieldPath, DCField field,
         QueryOperatorsEnum operatorEnum, Object parsedData) {
      List<OperatorValue> existingOperatorValues
            = this.entityFieldPathToOperatorValuesMap.get(entityFieldPath);
      if (existingOperatorValues == null) {
         existingOperatorValues = new ArrayList<OperatorValue>(2);
         this.entityFieldPathToOperatorValuesMap
               .put(entityFieldPath, existingOperatorValues);
      }
      existingOperatorValues.add(new OperatorValue(
            entityFieldPath, field, operatorEnum, parsedData));
   }
   
   /**
    * 
    * @return ordered
    */
   public Collection<List<OperatorValue>> getOperatorValues() {
      return this.entityFieldPathToOperatorValuesMap.values();
   }


   public void initBuildCriteria(String fulltextRegexSuffix) {
      this.criteria = new Criteria();
      this.fulltextRegexSuffix = fulltextRegexSuffix == null ? "" : fulltextRegexSuffix;
      this.sort = null;
   }
   
   public void enterCriteria(String entityFieldPath) {
      if (currentCriterias != null) {
         throw new IllegalArgumentException("Can't enter criteria when there is already one");
      }
      this.currentCriterias = new ArrayList<Criteria>(2); // most are single criteria field queries
      
      this.currentEntityFieldPath = entityFieldPath;
      this.currentOrigEntityFieldPath = entityFieldPath;
      
      // i18n language - add global or path-provided one if any :
      // TODO LATER also / rather by (country-specific) model / app / user...) default language...
      // HACK AFTER computing fulltext field :
      this.currentLanguage = this.getLanguageForValueFieldPath(this.currentEntityFieldPath); // NOT fieldOperatorStorageName !

      // criteria are added on mongo path :
      String fieldOperatorStorageName = this.getFieldOperatorStorageNameMap().get(entityFieldPath);
      if (fieldOperatorStorageName != null) {
         this.currentEntityFieldPath = fieldOperatorStorageName;
      }
      
      // init fulltextSorts :
      if (entityFieldPathToOperatorValuesMap.get(this.currentOrigEntityFieldPath).stream()
            .filter(operatorValue -> operatorValue.getOperatorEnum() == QueryOperatorsEnum.FULLTEXT)
            .count() != 0) {
         this.fulltextSorts = this.fulltextSorts == null ?
               new ArrayList<QueryOperatorsEnum>(3) : this.fulltextSorts;
         this.fulltextSorts.addAll(
               entityFieldPathToOperatorValuesMap.get(this.currentOrigEntityFieldPath).stream()
               .map(operatorValue -> operatorValue.getOperatorEnum())
               .filter(operatorEnum -> QueryOperatorsEnum.sortOperators.contains(operatorEnum))
               .collect(Collectors.toList()));
      }
      
      // TODO LATER check that, if sort, operators are not listOperators nor noValueOperators
   }
   
   public void exitCriteria() {
      if (currentCriterias == null) {
         throw new IllegalArgumentException("Can't exit criteria when there is none");
      }
      
      // parsing multiple values (of a field that is mentioned several times) :
      // (such as {limit=[10], founded=[>"-0143-04-01T00:00:00.000Z", <"-0043-04-02T00:00:00.000Z"]}
      // or /dc/type/sample.city.city?city:founded=>="0043-04-02T00:00:00.000Z"&city:founded=<="2043-04-02T00:00:00.000Z" )
      // NB. can't be done by merely chaining .and(...)'s because of mongo BasicDBObject limitations, see
      // http://www.mkyong.com/java/due-to-limitations-of-the-basicdbobject-you-cant-add-a-second-and/
      int criteriaNb = this.currentCriterias.size();
      if (criteriaNb == 0) {
         if (this.currentLanguage != null) {
            this.getCriteria().and(this.currentEntityFieldPath).is(currentLanguage);
         }
      } else {
         
         if (this.currentLanguage == null) {
            if (criteriaNb == 1) {
               addCriteria(this.currentCriterias.get(0), this.getCriteria());
            } else {
               this.getCriteria().andOperator(this.currentCriterias
                     .toArray(new Criteria[this.currentCriterias.size()]));
            }
            
         } else {
            // i18n language - add global or path-provided one if any :
            // TODO LATER also / rather by (country-specific) model / app / user...) default language...
            // HACK AFTER computing fulltext field :
            String entityPathPrefixAboveFulltext = this.currentEntityFieldPath
                  .replaceAll("\\." + DCI18nField.KEY_VALUE + '$', "");

            Criteria criteriaInsideElemMatch = null;
            if (criteriaNb == 1) {
               criteriaInsideElemMatch = this.currentCriterias.get(0).and(DCI18nField.KEY_LANGUAGE).is(currentLanguage);
            } else {
               criteriaInsideElemMatch = new Criteria(DCI18nField.KEY_LANGUAGE).is(currentLanguage)
                     .andOperator(this.currentCriterias.toArray(new Criteria[this.currentCriterias.size()]));
            }
            
            this.getCriteria().and(entityPathPrefixAboveFulltext).elemMatch(criteriaInsideElemMatch);
         }
      }
      
      this.currentEntityFieldPath = null;
      this.currentCriterias = null;
      this.currentLanguage = null;
   }

   private void addCriteria(Criteria toBeAdded, Criteria toBeAddedTo) {
      try {
         @SuppressWarnings("unchecked")
         List<Criteria> originalCriteriaChain = (List<Criteria>) criteriaChainField.get(toBeAddedTo);
         originalCriteriaChain.add(toBeAdded);
         criteriaChainField.set(toBeAdded, originalCriteriaChain);
      } catch (IllegalArgumentException | IllegalAccessException e) {
         throw new RuntimeException(e);
      }
      
      //toBeAddedTo.and(toBeAdded.getKey()).is(toBeAdded.getCriteriaObject().get(toBeAdded.getKey()));
   }

   public Criteria addCriteria() {
      // i18n language - add global or path-provided one if any :
      // TODO LATER also / rather by (country-specific) model / app / user...) default language...
      // HACK AFTER computing fulltext field :
      return addCriteria(this.currentLanguage == null ?
            this.currentEntityFieldPath : DCI18nField.KEY_VALUE);
   }

   /**
    * To use only when key is not currentEntityFieldPath ex. in case of default fulltext criteria
    * @param key
    * @return
    */
   public Criteria addCriteria(String key) {
      if (currentCriterias == null) {
         throw new IllegalArgumentException("Can't add criteria when there is none yet");
      }
      if (key == null) {
         throw new NullPointerException();
      }

      Criteria newCriteria = new Criteria(key);
      currentCriterias.add(newCriteria);
      return newCriteria;
   }


   
   /**
    * 
    * @param sortEnum if null, does nothing
    */
   public void addSort(QueryOperatorsEnum sortEnum) {
      if (sortEnum != null) {
         switch (sortEnum) {
         case SORT_ASC:
            this.addSort(new Sort(Direction.ASC, this.currentOrigEntityFieldPath));
            // (so that if fulltext, sorts on name rather than on _p._ft.v which would be meaningless
            // ex. otherwise Saint-Lô is after ex. Saint-Martin-de-Londres)
            // (even then, Saint-Lô is after ex. Saint-Loup-sur-Aujon => also do an exact match query first)
            break;
         case SORT_DESC:
            this.addSort(new Sort(Direction.DESC, this.currentOrigEntityFieldPath));
            // (so that if fulltext, sorts on name rather than on _p._ft.v which would be meaningless
            // ex. otherwise Saint-Lô is after Saint-Martin-de-Londres)
            // (even then, Saint-Lô is after ex. Saint-Loup-sur-Aujon => also do an exact match query first)
            break;
         default:
            break;
         }
      }
   }
   
   private void addSort(Sort currentSort) {
      this.sort = (this.sort == null) ? currentSort : this.sort.and(currentSort);
   }

   public Criteria getCriteria() {
      return this.criteria;
   }

   public Query getQuery() {
      return new Query(this.getCriteria());
   }

   public Sort getSort() {
      return this.sort;
   }

   public int getAggregatedQueryLimit() {
      return aggregatedQueryLimit;
   }

   public void setAggregatedQueryLimit(int aggregatedQueryLimit) {
      this.aggregatedQueryLimit = aggregatedQueryLimit;
   }

   public boolean isHasNoIndexedField() {
      return hasNoIndexedField;
   }

   public void setHasNoIndexedField(boolean hasNoIndexedField) {
      this.hasNoIndexedField = hasNoIndexedField;
   }

   public String getEntityFieldPath() {
      return currentEntityFieldPath;
   }

   public Set<String> getForbiddenMixins() {
      return forbiddenMixins;
   }

   public Set<String> getTopLevelAllowedMixins() {
      return topLevelAllowedMixins;
   }
   
   public boolean getRightsHaveToBeDecidedAtResourceLevel() {
      return !forbiddenMixins.isEmpty() || topLevelAllowedMixins.isEmpty();
   }

   
   public boolean checkLanguage(String language) {
      if (language.length() != 2) {
         return false;
      }
      return true;
   }
   
   public String getGlobalLanguage() {
      return globalLanguage;
   }

   public void setGlobalLanguage(String globalLanguage) {
      this.globalLanguage = globalLanguage;
   }

   /**
    * TODO LATER multiples (OR) languages
    * @param entityFieldPath with .v ex. i18n:name.v
    * @param language null if none specified
    * @return
    */
   public void setEntityFieldPathAsI18nValue(String entityValueFieldPath, String language) {
      String existingLanguage = entityFieldPathToLanguageMap.get(entityValueFieldPath);
      if (existingLanguage != null) {
         if (language != null) {
            this.addWarning("Found more than one language for field "
                  + entityValueFieldPath.replaceAll("^_p.", "") + " : " + existingLanguage + ", " + language);
         }
         return;
      }
      this.entityFieldPathToLanguageMap.put(entityValueFieldPath, language);
   }

   /**
    * 
    * @param entityFieldPath with .v ex. i18n:name.v
    * @return if entityFieldPath has been set as i18n, its own or else global language,
    * else null meaning no language criteria to add
    */
   public String getLanguageForValueFieldPath(String entityValueFieldPath) {
      if (isI18nValueFieldPath(entityValueFieldPath)) {
         String language = entityFieldPathToLanguageMap.get(entityValueFieldPath); // may be null meaning no language criteria
         if (language != null) {
            return language;
         }
         return globalLanguage;
      }
      return null;
   }

   public boolean isI18nValueFieldPath(String entityValueFieldPath) {
      return entityFieldPathToLanguageMap.containsKey(entityValueFieldPath);
   }

   public Map<String, String> getFieldOperatorStorageNameMap() {
      return this.fieldOperatorStorageNameMap ;
   }

   public String getFulltextRegexSuffix() {
      return this.fulltextRegexSuffix;
   }

   public boolean isFulltext() {
      return this.fulltextSorts != null;
   }

   public List<QueryOperatorsEnum> getFulltextSorts() {
      return this.fulltextSorts;
   }
   
}
