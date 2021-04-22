package com.wowza.wms.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wowza.util.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.http.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.vhost.*;

public class HTTPProviderMarathonDataInjection extends HTTProvider2Base
{
	private static final String CLASSNAME = "HTTPProviderMarathonDataInjection";
	private static final Class CLASS = HTTPProviderMarathonDataInjection.class;

	public void onBind(IVHost vhost, HostPort hostPort)
	{
		super.onBind(vhost, hostPort);
	}
	
	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp)
	{
		final ObjectMapper objectMapper = new ObjectMapper();
		if (!doHTTPAuthentication(vhost, req, resp)) {
			resp.setResponseCode(HttpStatus.SC_UNAUTHORIZED);
			resp.setHeader("Content-Type", "application/json");
			try {
				resp.getOutputStream()
					.write(("{ \"error\": \"Authentication required\" }").getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		try {
			final BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
			final StringBuilder sb = new StringBuilder();
			String buffer;
			while ((buffer = br.readLine()) != null) {
				sb.append(buffer);
			}
			final MetadataObject body = objectMapper.readValue(sb.toString().getBytes(), MetadataObject.class);

			final String appName = body.getLivename();
			final String appInstanceName = IApplicationInstance.DEFAULT_APPINSTANCE_NAME;
			if (appName == null || !vhost.applicationExists(appName)) {
				throw new Exception("Application not found");
			}
			final IApplicationInstance application = vhost.getApplication(appName).getAppInstance(appInstanceName);
			final MediaStreamMap streams = application.getStreams();
			if (streams == null) {
				WMSLoggerFactory.getLogger(CLASS).warn(CLASSNAME+".onHTTPRequest: No streams: "+appName+"/"+appInstanceName);
				throw new Exception("No one is streaming right now.");
			}
			IMediaStream stream = streams.getStream("alive");
			if (stream == null)
			{
				WMSLoggerFactory.getLogger(CLASS).warn(CLASSNAME+".onHTTPRequest: No stream named \"alive\"");
				throw new Exception("Stream 'alive' currently not online.");
			}
			injectFinisherMetadata(stream, objectMapper.writeValueAsString(body));
			
			resp.setResponseCode(HttpStatus.SC_OK);
			resp.setHeader("Content-Type", "application/json");
			resp.getOutputStream()
				.write(objectMapper.writeValueAsString(body).getBytes());
		} catch (final Exception exc) {
			WMSLoggerFactory.getLogger(CLASS).error(exc);
			resp.setResponseCode(HttpStatus.SC_BAD_REQUEST);
			resp.setHeader("Content-Type", "application/json");
			try {
				resp.getOutputStream()
					.write(("{ \"error\": \"" + exc.getMessage() + "\" }").getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void injectFinisherMetadata(IMediaStream stream, String payload)
	{
		try
		{
			// Create the AMF data structure
			// The wowzaConverter property and payload object allow Wowza Streaming Cloud to ingest the AMF metadata and convert it to ID3 
			AMFDataObj amfData = new AMFDataObj();
			amfData.put("wowzaConverter", "basic_string");
			amfData.put("payload", payload);

			WMSLoggerFactory.getLogger(CLASS).info("sendFinisherMetadata ["+stream.getContextStr()+"]");
			
			// Send the data event
			stream.sendDirect("onRaceFinisher", amfData);
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(CLASS).error(CLASSNAME+".sendFinisherMetadata["+stream.getContextStr()+"]: ", e);
		}
	}
}
