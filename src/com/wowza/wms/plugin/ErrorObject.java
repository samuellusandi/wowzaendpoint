package com.wowza.wms.plugin;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ErrorObject {
	
	private final String error;
}
