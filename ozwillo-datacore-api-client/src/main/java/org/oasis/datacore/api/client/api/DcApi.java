package org.oasis.datacore.api.client.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oasis.datacore.api.client.common.ApiException;
import org.oasis.datacore.api.client.common.ApiInvoker;
import org.oasis.datacore.api.client.model.DCResource;

public class DcApi {
	String basePath = "http://localhost:8080/";
	ApiInvoker apiInvoker = ApiInvoker.getInstance();

	public ApiInvoker getInvoker() {
		return apiInvoker;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getBasePath() {
		return basePath;
	}

	public String findModel() throws ApiException {
		// create path and map variables
		String path = "/dc/model/".replaceAll("\\{format\\}", "json");

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (String) ApiInvoker.deserialize(response, "", String.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> postAllDataInType(List<DCResource> body, String type) throws ApiException {
		// verify required params are set
		if (body == null || type == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/type/{type}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, body, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> putAllDataInType(List<DCResource> body, String type) throws ApiException {
		// verify required params are set
		if (body == null || type == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/type/{type}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "PUT", queryParams, body, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> findDataInType(String type, String queryParameters, Integer start, Integer limit) throws ApiException {
		// verify required params are set
		if (type == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/type/{type}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		if (!"null".equals(String.valueOf(start)))
			queryParams.put("start", String.valueOf(start));
		if (!"null".equals(String.valueOf(limit)))
			queryParams.put("limit", String.valueOf(limit));
		if (!"null".equals(String.valueOf(queryParameters)))
			queryParams.put("#queryParameters", String.valueOf(queryParameters));
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> postAllData(List<DCResource> body) throws ApiException {
		// verify required params are set
		if (body == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/".replaceAll("\\{format\\}", "json");

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, body, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> putAllData(List<DCResource> body) throws ApiException {
		// verify required params are set
		if (body == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/".replaceAll("\\{format\\}", "json");

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "PUT", queryParams, body, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> findData(String queryParameters, Integer start, Integer limit) throws ApiException {
		// create path and map variables
		String path = "/dc/".replaceAll("\\{format\\}", "json");

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		if (!"null".equals(String.valueOf(start)))
			queryParams.put("start", String.valueOf(start));
		if (!"null".equals(String.valueOf(limit)))
			queryParams.put("limit", String.valueOf(limit));
		if (!"null".equals(String.valueOf(queryParameters)))
			queryParams.put("#queryParameters", String.valueOf(queryParameters));
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public DCResource putDataInType(DCResource body, String type, String iri) throws ApiException {
		// verify required params are set
		if (body == null || type == null || iri == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/type/{type}/{iri:.+}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()))
				.replaceAll("\\{" + "iri" + "\\}", apiInvoker.escapeString(iri.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "PUT", queryParams, body, headerParams, contentType);
			if (response != null) {
				return (DCResource) ApiInvoker.deserialize(response, "", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public DCResource getData(String type, String iri, String IfNoneMatch) throws ApiException {
		// verify required params are set
		if (type == null || iri == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/type/{type}/{iri:.+}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()))
				.replaceAll("\\{" + "iri" + "\\}", apiInvoker.escapeString(iri.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		headerParams.put("If-None-Match", IfNoneMatch);
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (DCResource) ApiInvoker.deserialize(response, "", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public DCResource deleteData(String type, String iri, String IfMatch) throws ApiException {
		// verify required params are set
		if (type == null || iri == null || IfMatch == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/type/{type}/{iri:.+}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()))
				.replaceAll("\\{" + "iri" + "\\}", apiInvoker.escapeString(iri.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		headerParams.put("If-Match", IfMatch);
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "DELETE", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (DCResource) ApiInvoker.deserialize(response, "", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public DCResource postDataInTypeOnGet(String type, String method) throws ApiException {
		// verify required params are set
		if (type == null || method == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/post/type/{type}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		if (!"null".equals(String.valueOf(method)))
			queryParams.put("method", String.valueOf(method));
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (DCResource) ApiInvoker.deserialize(response, "", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public DCResource putPatchDeleteDataOnGet(String type, String iri, String method) throws ApiException {
		// verify required params are set
		if (type == null || iri == null || method == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/change/type/{type}/{iri:.+}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()))
				.replaceAll("\\{" + "iri" + "\\}", apiInvoker.escapeString(iri.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		if (!"null".equals(String.valueOf(method)))
			queryParams.put("method", String.valueOf(method));
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (DCResource) ApiInvoker.deserialize(response, "", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> queryDataInType(String type, String query, String language) throws ApiException {
		// verify required params are set
		if (type == null) {
			throw new ApiException(400, "missing required params");
		}
		// create path and map variables
		String path = "/dc/query/type/{type}".replaceAll("\\{format\\}", "json").replaceAll("\\{" + "type" + "\\}", apiInvoker.escapeString(type.toString()));

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		if (!"null".equals(String.valueOf(query)))
			queryParams.put("query", String.valueOf(query));
		if (!"null".equals(String.valueOf(language)))
			queryParams.put("language", String.valueOf(language));
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}

	public List<DCResource> queryData(String query, String language) throws ApiException {
		// create path and map variables
		String path = "/dc/query".replaceAll("\\{format\\}", "json");

		// query params
		Map<String, String> queryParams = new HashMap<String, String>();
		Map<String, String> headerParams = new HashMap<String, String>();

		if (!"null".equals(String.valueOf(query)))
			queryParams.put("query", String.valueOf(query));
		if (!"null".equals(String.valueOf(language)))
			queryParams.put("language", String.valueOf(language));
		String contentType = "application/json";

		try {
			String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, null, headerParams, contentType);
			if (response != null) {
				return (List<DCResource>) ApiInvoker.deserialize(response, "Array", DCResource.class);
			} else {
				return null;
			}
		} catch (ApiException ex) {
			if (ex.getCode() == 404) {
				return null;
			} else {
				throw ex;
			}
		}
	}
}
