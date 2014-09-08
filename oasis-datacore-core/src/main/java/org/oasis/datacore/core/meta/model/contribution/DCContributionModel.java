package org.oasis.datacore.core.meta.model.contribution;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCMixin;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.model.DCSecurity;

public class DCContributionModel extends DCModel {
	
	private final static String CONTRIBUTION_COLLECTION_SUFFIX = ".c";
	
	private DCModel delegate;

	public DCContributionModel(DCModel delegate) {
		this.delegate = delegate;
		this.delegate.addMixin(new DCContributionMixin());
		this.setHistorizable(false);
		this.setSecurity(new DCContributionSecurity(this.getSecurity()));
	}

	@Override
	public String getCollectionName() {
		return delegate.getCollectionName() + CONTRIBUTION_COLLECTION_SUFFIX;
	}
	
	// delegate methods :
	
	public int hashCode() {
		return delegate.hashCode();
	}

	public DCSecurity getSecurity() {
		return delegate.getSecurity();
	}

	public void setSecurity(DCSecurity security) {
		delegate.setSecurity(security);
	}

	public String getDocumentation() {
		return delegate.getDocumentation();
	}

	public DCField getField(String name) {
		return delegate.getField(name);
	}

	public Map<String, DCModelBase> getGlobalMixinMap() {
		return delegate.getGlobalMixinMap();
	}

	public Set<String> getGlobalMixinNames() {
		return delegate.getGlobalMixinNames();
	}

	public Map<String, DCField> getGlobalFieldMap() {
		return delegate.getGlobalFieldMap();
	}

	public boolean hasMixin(String name) {
		return delegate.hasMixin(name);
	}

	public DCField getGlobalField(String name) {
		return delegate.getGlobalField(name);
	}

	public void resetGlobalCaches() {
		delegate.resetGlobalCaches();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public String getName() {
		return delegate.getName();
	}

	public Map<String, DCField> getFieldMap() {
		return delegate.getFieldMap();
	}

	public Collection<DCModelBase> getMixins() {
		return delegate.getMixins();
	}

	public List<Object> getListeners() {
		return delegate.getListeners();
	}

	public DCModelBase addField(DCField field) {
		return delegate.addField(field);
	}

	public DCModelBase addMixin(DCModelBase mixin) {
		return delegate.addMixin(mixin);
	}

	public DCModelBase addListener(Object listener) {
		return delegate.addListener(listener);
	}

	public void setName(String name) {
		delegate.setName(name);
	}

	public void setDocumentation(String documentation) {
		delegate.setDocumentation(documentation);
	}

	public void setFieldMap(Map<String, DCField> fieldMap) {
		delegate.setFieldMap(fieldMap);
	}

	public void setListeners(List<Object> listeners) {
		delegate.setListeners(listeners);
	}

	public boolean isHistorizable() {
		return delegate.isHistorizable();
	}

	public void setHistorizable(boolean isHistorizable) {
		delegate.setHistorizable(isHistorizable);
	}

	public String toString() {
		return delegate.toString();
	}

   public int getMaxScan() {
      return delegate.getMaxScan();
   }

   public void setMaxScan(int maxScan) {
      delegate.setMaxScan(maxScan);
   }

   public Collection<DCModelBase> getGlobalMixins() {
      return delegate.getGlobalMixins();
   }

   public Collection<DCField> getGlobalFields() {
      return delegate.getGlobalFields();
   }

   public long getMajorVersion() {
      return delegate.getMajorVersion();
   }

   public long getVersion() {
      return delegate.getVersion();
   }

   public Set<String> getFieldNames() {
      return delegate.getFieldNames();
   }

   public Collection<DCField> getFields() {
      return delegate.getFields();
   }

   public Set<String> getMixinNames() {
      return delegate.getMixinNames();
   }

   public Map<String, DCModelBase> getMixinMap() {
      return delegate.getMixinMap();
   }

   public List<Object> getFieldAndMixins() {
      return delegate.getFieldAndMixins();
   }

   public DCModelBase addMixins(DCMixin... mixins) {
      return delegate.addMixins(mixins);
   }

   public void setMajorVersion(long majorVersion) {
      delegate.setMajorVersion(majorVersion);
   }

   public void setVersion(long version) {
      delegate.setVersion(version);
   }

   public void setMixinMap(LinkedHashMap<String, DCModelBase> mixinMap) {
      delegate.setMixinMap(mixinMap);
   }

   public boolean isContributable() {
      return delegate.isContributable();
   }

   public void setContributable(boolean isContributable) {
      delegate.setContributable(isContributable);
   }

}
