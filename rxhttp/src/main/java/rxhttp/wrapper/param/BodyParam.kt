package rxhttp.wrapper.param

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.ByteString
import rxhttp.wrapper.OkHttpCompat
import rxhttp.wrapper.entity.FileRequestBody
import rxhttp.wrapper.utils.BuildUtil
import java.io.File

/**
 * User: ljx
 * Date: 2019-09-11
 * Time: 11:52
 *
 * @param url    request url
 * @param method [Method.POST]、[Method.PUT]、[Method.DELETE]、[Method.PATCH]
 */
class BodyParam(
    url: String,
    method: Method,
) : AbstractBodyParam<BodyParam>(url, method) {

    private var jsonValue: Any? = null
    private var requestBody: RequestBody? = null

    fun setJsonBody(value: Any): BodyParam {
        jsonValue = value
        requestBody = null
        return this
    }

    fun setBody(requestBody: RequestBody): BodyParam {
        this.requestBody = requestBody
        jsonValue = null
        return this
    }

    @JvmOverloads
    fun setBody(
        content: String,
        mediaType: MediaType? = null,
    ): BodyParam = setBody(OkHttpCompat.create(mediaType, content))

    @JvmOverloads
    fun setBody(
        content: ByteString,
        mediaType: MediaType? = null,
    ): BodyParam = setBody(OkHttpCompat.create(mediaType, content))

    @JvmOverloads
    fun setBody(
        content: ByteArray,
        mediaType: MediaType? = null,
        offset: Int = 0,
        byteCount: Int = content.size,
    ): BodyParam = setBody(OkHttpCompat.create(mediaType, content, offset, byteCount))

    @JvmOverloads
    fun setBody(
        file: File,
        mediaType: MediaType? = BuildUtil.getMediaType(file.name),
    ): BodyParam = setBody(FileRequestBody(file, 0, mediaType))

    override fun getRequestBody(): RequestBody {
        jsonValue?.let { requestBody = convert(it) }
        return requestBody
            ?: throw NullPointerException("requestBody cannot be null, please call the setBody series methods")
    }

    override fun add(key: String, value: Any) = this
}