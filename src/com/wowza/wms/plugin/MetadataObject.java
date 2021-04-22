package com.wowza.wms.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataObject {

	@JsonProperty("livename")
	private String livename;
	@JsonProperty("type")
	private String type;
	@JsonProperty("information")
	private Map<String, String> information;
}
