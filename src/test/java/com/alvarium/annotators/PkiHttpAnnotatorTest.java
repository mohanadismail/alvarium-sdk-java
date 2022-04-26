
/*******************************************************************************
 * Copyright 2022 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.alvarium.annotators;

import java.util.HashMap;
import java.util.Date;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import com.alvarium.SdkInfo;
import com.alvarium.annotators.http.Ed2551RequestHandler;
import com.alvarium.annotators.http.RequestHandlerException;
import com.alvarium.contracts.Annotation;
import com.alvarium.contracts.AnnotationType;
import com.alvarium.contracts.DerivedComponent;
import com.alvarium.hash.HashInfo;
import com.alvarium.hash.HashType;
import com.alvarium.sign.KeyInfo;
import com.alvarium.sign.SignType;
import com.alvarium.sign.SignatureInfo;
import com.alvarium.utils.ImmutablePropertyBag;
import com.alvarium.utils.PropertyBag;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PkiHttpAnnotatorTest {
  final AnnotatorFactory annotatorFactory = new AnnotatorFactory();
  final KeyInfo pubKey = new KeyInfo("./src/test/java/com/alvarium/annotators/public.key",
      SignType.Ed25519);
  final KeyInfo privKey = new KeyInfo("./src/test/java/com/alvarium/annotators/private.key",
      SignType.Ed25519);
  final SignatureInfo sigInfo = new SignatureInfo(pubKey, privKey);
  final byte[] data = String.format("{key: \"test\"}").getBytes();

  HttpPost getRequest(SignatureInfo sigInfo) throws RequestHandlerException {
    HttpPost request = new HttpPost(URI.create("http://example.com/foo?var1=&var2=2"));
    Date date = new Date();
    request.setHeader("Date", date.toString());
    request.setHeader("Content-Type", "application/json");
    request.setHeader("Content-Length", "18");
    String[] fields = { DerivedComponent.METHOD.getValue(),
        DerivedComponent.PATH.getValue(),
        DerivedComponent.AUTHORITY.getValue(),
        "Content-Type", "Content-Length" };
    Ed2551RequestHandler requestHandler = new Ed2551RequestHandler(request);
    requestHandler.addSignatureHeaders(date, fields, sigInfo);
    return request;
  }

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  // Tests the Signature signed by the assembler
  public void testAnnotationOK() throws AnnotatorException, RequestHandlerException {
    HttpPost request = getRequest(sigInfo);
    try {
      request.setEntity(new StringEntity("{key: \"test\"}"));
    } catch (UnsupportedEncodingException e) {
      throw new AnnotatorException("Unsupported Character Encoding", e);
    }

    HashMap<String, Object> map = new HashMap<>();
    map.put(AnnotationType.PKIHttp.name(), request);
    final PropertyBag ctx = new ImmutablePropertyBag(map);

    final AnnotationType[] annotators = { AnnotationType.PKIHttp };
    final SdkInfo config = new SdkInfo(annotators, new HashInfo(HashType.SHA256Hash), sigInfo, null);
    final Annotator annotator = annotatorFactory.getAnnotator(AnnotationType.PKIHttp, config);
    final Annotation annotation = annotator.execute(ctx, data);
    assertTrue("isSatisfied should be true", annotation.getIsSatisfied());
  }

  @Test
  public void testInvalidKeyType() throws AnnotatorException, RequestHandlerException {
    final String signatureInput = "\"@method\" \"@path\" \"@authority\" \"Content-Type\" " + 
    "\"Content-Length\";created=1646146637;keyid=\"public.key\";alg=\"invalid\"";

    HttpPost request = getRequest(sigInfo);
    try {
      request.setEntity(new StringEntity("{key: \"test\"}"));
    } catch (UnsupportedEncodingException e) {
      throw new AnnotatorException("Unsupported Character Encoding", e);

    }
    request.setHeader("Signature-Input", signatureInput);

    HashMap<String, Object> map = new HashMap<>();
    map.put(AnnotationType.PKIHttp.name(), request);
    final PropertyBag ctx = new ImmutablePropertyBag(map);

    exceptionRule.expect(AnnotatorException.class);
    exceptionRule.expectMessage("Invalid key type invalid");

    final AnnotationType[] annotators = { AnnotationType.PKIHttp };
    final SdkInfo config = new SdkInfo(annotators, new HashInfo(HashType.SHA256Hash), sigInfo, null);
    final Annotator annotator = annotatorFactory.getAnnotator(AnnotationType.PKIHttp, config);
    annotator.execute(ctx, data);
  }

  @Test
  public void testKeyNotFound() throws AnnotatorException, RequestHandlerException {
    final String signatureInput = "\"@method\" \"@path\" \"@authority\" \"Content-Type\" " + 
    "\"Content-Length\";created=1646146637;keyid=\"invalid\";alg=\"ed25519\"";

    HttpPost request = getRequest(sigInfo);
    try {
      request.setEntity(new StringEntity("{key: \"test\"}"));
    } catch (UnsupportedEncodingException e) {
      throw new AnnotatorException("Unsupported Character Encoding", e);

    }
    request.setHeader("Signature-Input", signatureInput);

    HashMap<String, Object> map = new HashMap<>();
    map.put(AnnotationType.PKIHttp.name(), request);
    final PropertyBag ctx = new ImmutablePropertyBag(map);

    exceptionRule.expect(AnnotatorException.class);
    exceptionRule.expectMessage("Failed to load public key");

    final AnnotationType[] annotators = { AnnotationType.PKIHttp };
    final SdkInfo config = new SdkInfo(annotators, new HashInfo(HashType.SHA256Hash), sigInfo, null);
    final Annotator annotator = annotatorFactory.getAnnotator(AnnotationType.PKIHttp, config);
    annotator.execute(ctx, data);
  }

  @Test
  public void testEmptySignature() throws AnnotatorException, RequestHandlerException {
    final String signature = "";

    HttpPost request = getRequest(sigInfo);
    try {
      request.setEntity(new StringEntity("{key: \"test\"}"));
    } catch (UnsupportedEncodingException e) {
      throw new AnnotatorException("Unsupported Character Encoding", e);

    }
    request.setHeader("Signature", signature);

    HashMap<String, Object> map = new HashMap<>();
    map.put(AnnotationType.PKIHttp.name(), request);
    final PropertyBag ctx = new ImmutablePropertyBag(map);

    final AnnotationType[] annotators = { AnnotationType.PKIHttp };
    final SdkInfo config = new SdkInfo(annotators, new HashInfo(HashType.SHA256Hash), sigInfo, null);
    final Annotator annotator = annotatorFactory.getAnnotator(AnnotationType.PKIHttp, config);
    final Annotation annotation = annotator.execute(ctx, data);
    assertFalse("isSatisfied should be false", annotation.getIsSatisfied());
  }

  @Test
  public void testInvalidSignature() throws AnnotatorException, RequestHandlerException {
    final String signature = "invalid";

    HttpPost request = getRequest(sigInfo);
    try {
      request.setEntity(new StringEntity("{key: \"test\"}"));
    } catch (UnsupportedEncodingException e) {
      throw new AnnotatorException("Unsupported Character Encoding", e);

    }
    request.setHeader("Signature", signature);

    HashMap<String, Object> map = new HashMap<>();
    map.put(AnnotationType.PKIHttp.name(), request);
    final PropertyBag ctx = new ImmutablePropertyBag(map);

    final AnnotationType[] annotators = { AnnotationType.PKIHttp };
    final SdkInfo config = new SdkInfo(annotators, new HashInfo(HashType.SHA256Hash), sigInfo, null);
    final Annotator annotator = annotatorFactory.getAnnotator(AnnotationType.PKIHttp, config);
    final Annotation annotation = annotator.execute(ctx, data);
    assertFalse("isSatisfied should be false", annotation.getIsSatisfied());
  }
}
