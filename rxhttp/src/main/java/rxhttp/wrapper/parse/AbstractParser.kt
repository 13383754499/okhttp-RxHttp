package rxhttp.wrapper.parse

import com.google.gson.internal.`$Gson$Types`
import okhttp3.Response
import rxhttp.wrapper.utils.convert
import rxhttp.wrapper.utils.getActualTypeParameter
import java.io.IOException
import java.lang.reflect.Type

/**
 * User: ljx
 * Date: 2019/1/21
 * Time: 15:32
 */
@Deprecated(
    "This supports only single type, TypeParser supports multiple type",
    replaceWith = ReplaceWith("TypeParser<T>")
)
abstract class AbstractParser<T> : Parser<T> {
    @JvmField
    protected var mType: Type

    constructor() {
        mType = getActualTypeParameter(javaClass, 0)
    }

    constructor(type: Type) {
        mType = `$Gson$Types`.canonicalize(type)
    }

    @Deprecated("", replaceWith = ReplaceWith("response.convertTo(rawType, types)"))
    @Throws(IOException::class)
    fun <R> convert(response: Response, type: Type): R = response.convert(type)
}