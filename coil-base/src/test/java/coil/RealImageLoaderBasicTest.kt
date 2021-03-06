package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.decode.Options
import coil.fetch.Fetcher
import coil.request.Parameters
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.transform.Transformation
import coil.util.createBitmap
import coil.util.createGetRequest
import coil.util.createLoadRequest
import coil.util.toDrawable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Basic tests for [RealImageLoader] that don't touch Android's graphics pipeline ([BitmapFactory], [ImageDecoder], etc.).
 */
@RunWith(RobolectricTestRunner::class)
class RealImageLoaderBasicTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        imageLoader = ImageLoader(context) as RealImageLoader
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun `isCachedDrawableValid - fill`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(200, 200),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(150, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 100),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 200),
            scale = Scale.FILL,
            request = request
        ))
    }

    @Test
    fun `isCachedDrawableValid - fit`() {
        val request = createGetRequest {
            size(100, 100)
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(200, 200),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(150, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 100),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 800),
            scale = Scale.FIT,
            request = request
        ))
    }

    @Test
    fun `isCachedDrawableValid - small not sampled cached drawable is valid`() {
        val request = createGetRequest {
            precision(Precision.INEXACT)
        }
        val cached = createBitmap().toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = false,
            size = PixelSize(200, 200),
            scale = Scale.FILL,
            request = request
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCachedDrawableValid - cached drawable with equal or greater config is valid`() {
        val request = createGetRequest()

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return imageLoader.isCachedDrawableValid(
                cached = cached,
                isSampled = true,
                size = PixelSize(100, 100),
                scale = Scale.FILL,
                request = request
            )
        }

        assertTrue(isBitmapConfigValid(Bitmap.Config.RGBA_F16))
        assertTrue(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
        assertFalse(isBitmapConfigValid(Bitmap.Config.RGB_565))
        assertFalse(isBitmapConfigValid(Bitmap.Config.ALPHA_8))
    }

    @Test
    fun `isCachedDrawableValid - allowRgb565=true allows using RGB_565 bitmap with ARGB_8888 request`() {
        val request = createGetRequest {
            allowRgb565(true)
        }
        val cached = createBitmap(config = Bitmap.Config.HARDWARE).toDrawable(context)
        val isValid = imageLoader.isCachedDrawableValid(
            cached = cached,
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FILL,
            request = request
        )
        assertTrue(isValid)
    }

    @Test
    fun `isCachedDrawableValid - allowHardware=false prevents using cached hardware bitmap`() {
        val request = createGetRequest {
            allowHardware(false)
        }

        fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
            val cached = createBitmap(config = config).toDrawable(context)
            return imageLoader.isCachedDrawableValid(
                cached = cached,
                isSampled = true,
                size = PixelSize(100, 100),
                scale = Scale.FILL,
                request = request
            )
        }

        assertFalse(isBitmapConfigValid(Bitmap.Config.HARDWARE))
        assertTrue(isBitmapConfigValid(Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `isCachedDrawableValid - exact precision`() {
        val request = createLoadRequest(context) {
            precision(Precision.EXACT)
        }
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(50, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 50),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 50),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FILL,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 100, height = 100).toDrawable(context),
            isSampled = true,
            size = PixelSize(100, 100),
            scale = Scale.FIT,
            request = request
        ))
        assertTrue(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 400, height = 200).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 200),
            scale = Scale.FILL,
            request = request
        ))
        assertFalse(imageLoader.isCachedDrawableValid(
            cached = createBitmap(width = 200, height = 400).toDrawable(context),
            isSampled = true,
            size = PixelSize(400, 800),
            scale = Scale.FIT,
            request = request
        ))
    }

    @Test
    fun `computeCacheKey - null key`() {
        val fetcher = createFakeFetcher(key = null)
        val key = imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList())

        assertNull(key)
    }

    @Test
    fun `computeCacheKey - basic key`() {
        val fetcher = createFakeFetcher()
        val result = imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, emptyList())

        assertEquals("base_key", result)
    }

    @Test
    fun `computeCacheKey - params only`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val result = imageLoader.computeCacheKey(fetcher, Unit, parameters, emptyList())

        assertEquals("base_key#key2=cached2#key3=cached3", result)
    }

    @Test
    fun `computeCacheKey - transformations only`() {
        val fetcher = createFakeFetcher()
        val transformations = createFakeTransformations()
        val result = imageLoader.computeCacheKey(fetcher, Unit, Parameters.EMPTY, transformations)

        assertEquals("base_key#key1#key2", result)
    }

    @Test
    fun `computeCacheKey - complex key`() {
        val fetcher = createFakeFetcher()
        val parameters = createFakeParameters()
        val transformations = createFakeTransformations()
        val result = imageLoader.computeCacheKey(fetcher, Unit, parameters, transformations)

        assertEquals("base_key#key2=cached2#key3=cached3#key1#key2", result)
    }

    private fun createFakeTransformations(): List<Transformation> {
        return listOf(
            object : Transformation {
                override fun key() = "key1"
                override suspend fun transform(pool: BitmapPool, input: Bitmap) = throw UnsupportedOperationException()
            },
            object : Transformation {
                override fun key() = "key2"
                override suspend fun transform(pool: BitmapPool, input: Bitmap) = throw UnsupportedOperationException()
            }
        )
    }

    private fun createFakeParameters(): Parameters {
        return Parameters.Builder()
            .set("key1", "no_cache")
            .set("key2", "cached2", "cached2")
            .set("key3", "cached3", "cached3")
            .build()
    }

    private fun createFakeFetcher(key: String? = "base_key"): Fetcher<Any> {
        return object : Fetcher<Any> {
            override fun key(data: Any) = key

            override suspend fun fetch(
                pool: BitmapPool,
                data: Any,
                size: Size,
                options: Options
            ) = throw UnsupportedOperationException()
        }
    }
}
