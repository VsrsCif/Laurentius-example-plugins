/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.laurentius.interceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import si.laurentius.commons.cxf.SoapUtils;
import si.laurentius.commons.enums.MimeValue;
import si.laurentius.commons.pmode.EBMSMessageContext;

import si.laurentius.commons.utils.SEDLogger;
import si.laurentius.commons.utils.StorageUtils;
import si.laurentius.ebox.SEDBox;
import si.laurentius.interceptor.exception.EBMSFault;
import si.laurentius.interceptor.exception.EBMSFaultCode;
import si.laurentius.msh.inbox.mail.MSHInMail;
import si.laurentius.msh.inbox.payload.MSHInPart;
import si.laurentius.msh.outbox.mail.MSHOutMail;
import si.laurentius.plugin.interceptor.MailInterceptorDef;
import si.laurentius.plugin.interceptor.MailInterceptorPropertyDef;
import si.laurentius.plugin.interfaces.PropertyType;
import si.laurentius.plugin.interfaces.SoapInterceptorInterface;

/**
 *
 * @author Jože Rihtaršič
 */
@Stateless
@Local(SoapInterceptorInterface.class)
public class ExampleInterceptor implements SoapInterceptorInterface {

  public static final String KEY_FOLDER = "example.interceptor.folder";
  public static final String KEY_PARAM_1 = "example.interceptor.parameter.001";
  public static final String KEY_PARAM_2 = "example.interceptor.parameter.002";
  public static final String KEY_PARAM_3 = "example.interceptor.parameter.003";
  private static final SEDLogger LOG = new SEDLogger(ExampleInterceptor.class);

  @Override
  public boolean handleMessage(SoapMessage msg,
          Properties contextProperties) throws Fault {

    long l = LOG.logStart();

    LOG.formatedlog("Start interceptor: '%s' " + getDefinition().getName());
    for (String pKey : contextProperties.stringPropertyNames()) {
      LOG.formatedlog("Key: '%s', value: '%s' ", pKey, contextProperties.
              getProperty(pKey));

    }

    
    MSHInMail inMail = SoapUtils.getMSHInMail(msg);

    

    // inmail request
    if (SoapUtils.isRequestMessage(msg) && inMail != null) {
      LOG.formatedlog("Got inmail request from: %s, receiver:  %s, service: %s", inMail.getSenderEBox(), inMail.getReceiverEBox(), inMail.getService());
      
      // test if inmail has json attachment
      for (MSHInPart ip : inMail.getMSHInPayload().getMSHInParts()){
        
        if (Objects.equals( MimeValue.MIME_JSON.getMimeType(), ip.getMimeType())) {
          File fp = StorageUtils.getFile(ip.getFilepath());

          try (JsonReader rdr = Json.createReader(new FileInputStream(fp))) {
            JsonObject obj = rdr.readObject();
            for (Map.Entry<String, JsonValue> e: obj.entrySet()) {
              LOG.formatedlog("Got json data: %s, %s", e.getKey(), e.getValue().toString());
            }
            
          } catch (FileNotFoundException ex) {
            String message = String.format("Error occured while reading json file: %s",ex.getMessage(), ex);
            throw new EBMSFault(EBMSFaultCode.PayloadError, inMail.getMessageId(),
              message, SoapFault.FAULT_CODE_CLIENT);
          }

        }
      }

    }


    LOG.logEnd(l);
    return true;

  }

  @Override
  public void handleFault(org.apache.cxf.binding.soap.SoapMessage t,
          Properties contextProperties) {

  }

  @Override
  public MailInterceptorDef getDefinition() {
    MailInterceptorDef tt = new MailInterceptorDef();
    tt.setType("example-interceptor");
    tt.setName("Example Interceptor plugin");
    tt.setDescription("This is simple example of interceptor");

    tt.getMailInterceptorPropertyDeves().add(createTTProperty(KEY_FOLDER,
            "Example folder", true, PropertyType.String.getType(), null, null));
    tt.getMailInterceptorPropertyDeves().add(
            createTTProperty(KEY_PARAM_1, "First parameter", true,
                    PropertyType.Integer.getType(), null, null));
    tt.getMailInterceptorPropertyDeves().add(
            createTTProperty(KEY_PARAM_2, "Second parameter (true/false)", true,
                    PropertyType.Boolean.getType(), null, null));
    tt.getMailInterceptorPropertyDeves().add(
            createTTProperty(KEY_PARAM_3, "Thrid parameter", false,
                    PropertyType.String.getType(), null, null));
    return tt;
  }

  private MailInterceptorPropertyDef createTTProperty(String key, String desc,
          boolean mandatory,
          String type, String valFormat, String valList) {
    MailInterceptorPropertyDef ttp = new MailInterceptorPropertyDef();
    ttp.setKey(key);
    ttp.setDescription(desc);
    ttp.setMandatory(mandatory);
    ttp.setType(type);
    ttp.setValueFormat(valFormat);
    ttp.setValueList(valList);
    return ttp;
  }

}
