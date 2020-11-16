package rxhttp.wrapper.param;

import java.lang.Object;
import java.lang.String;
import org.jetbrains.annotations.NotNull;

/**
 * 本类由@Converter、@Domain、@OkClient注解中的className字段生成  类命名方式: Rx + {className字段值} + Http
 * Github
 * https://github.com/liujingxing/RxHttp
 * https://github.com/liujingxing/RxLife
 * https://github.com/liujingxing/okhttp-RxHttp/wiki/FAQ
 * https://github.com/liujingxing/okhttp-RxHttp/wiki/更新日志
 */
public class RxSimpleHttp {
  /**
   * 本类所有方法都会调用本方法
   */
  private static <R extends RxHttp> void wrapper(R rxHttp) {
    rxHttp.setSimpleClient();
    rxHttp.setDomainToUpdateIfAbsent();
  }

  public static RxHttpNoBodyParam get(String url, Object... formatArgs) {
    RxHttpNoBodyParam rxHttp = RxHttp.get(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpNoBodyParam head(String url, Object... formatArgs) {
    RxHttpNoBodyParam rxHttp = RxHttp.head(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpFormParam postForm(String url, Object... formatArgs) {
    RxHttpFormParam rxHttp = RxHttp.postForm(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpFormParam putForm(String url, Object... formatArgs) {
    RxHttpFormParam rxHttp = RxHttp.putForm(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpFormParam patchForm(String url, Object... formatArgs) {
    RxHttpFormParam rxHttp = RxHttp.patchForm(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpFormParam deleteForm(String url, Object... formatArgs) {
    RxHttpFormParam rxHttp = RxHttp.deleteForm(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonParam postJson(String url, Object... formatArgs) {
    RxHttpJsonParam rxHttp = RxHttp.postJson(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonParam putJson(String url, Object... formatArgs) {
    RxHttpJsonParam rxHttp = RxHttp.putJson(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonParam patchJson(String url, Object... formatArgs) {
    RxHttpJsonParam rxHttp = RxHttp.patchJson(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonParam deleteJson(String url, Object... formatArgs) {
    RxHttpJsonParam rxHttp = RxHttp.deleteJson(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonArrayParam postJsonArray(String url, Object... formatArgs) {
    RxHttpJsonArrayParam rxHttp = RxHttp.postJsonArray(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonArrayParam putJsonArray(String url, Object... formatArgs) {
    RxHttpJsonArrayParam rxHttp = RxHttp.putJsonArray(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonArrayParam patchJsonArray(String url, Object... formatArgs) {
    RxHttpJsonArrayParam rxHttp = RxHttp.patchJsonArray(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpJsonArrayParam deleteJsonArray(String url, Object... formatArgs) {
    RxHttpJsonArrayParam rxHttp = RxHttp.deleteJsonArray(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpPostEncryptJsonParam postEncryptJson(@NotNull String url,
      Object... formatArgs) {
    RxHttpPostEncryptJsonParam rxHttp = RxHttp.postEncryptJson(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpPostEncryptFormParam postEncryptForm(String url, Object... formatArgs) {
    RxHttpPostEncryptFormParam rxHttp = RxHttp.postEncryptForm(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpPostEncryptFormParam postEncryptForm(String url, Method method,
      Object... formatArgs) {
    RxHttpPostEncryptFormParam rxHttp = RxHttp.postEncryptForm(url, method, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpPostText postText(String url, Object... formatArgs) {
    RxHttpPostText rxHttp = RxHttp.postText(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpGetEncryptParam getEncrypt(String url, Object... formatArgs) {
    RxHttpGetEncryptParam rxHttp = RxHttp.getEncrypt(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpPostBodyParam postBodyForm(String url, Object... formatArgs) {
    RxHttpPostBodyParam rxHttp = RxHttp.postBodyForm(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }

  public static RxHttpPostEncryptJsonParam1 postEncryptJson1(String url, Object... formatArgs) {
    RxHttpPostEncryptJsonParam1 rxHttp = RxHttp.postEncryptJson1(url, formatArgs);
    wrapper(rxHttp);
    return rxHttp;
  }
}
