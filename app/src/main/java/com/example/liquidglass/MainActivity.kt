package com.example.liquidglass

import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.RuntimeShader
import kotlinx.coroutines.delay
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent { LiquidGlassApp() }
    }
}

private val glassShaderSource = """
uniform shader backdrop;
uniform float2 size;
uniform float time;
uniform float2 focus;
half4 main(float2 p) {
    float2 uv = p / size;
    float2 q = (p - focus) / float2(size.x * 0.54, size.y * 0.95);
    float r = length(q);
    float lens = exp(-r * 2.8);
    float2 normal = normalize(q + float2(0.0001, 0.0001));
    float2 bend = normal * (7.0 * lens) + float2(0.0, sin(time * 1.1 + p.x * 0.018) * 0.65);
    half4 scene = backdrop.eval(p + bend);
    float edge = smoothstep(0.76, 1.02, r);
    float sweep = sin((uv.x * 1.7 + uv.y * 0.55) * 6.283 + time * 0.65);
    float spec = pow(max(0.0, 1.0 - abs(uv.y - 0.16 - 0.035 * sin(uv.x * 6.0 + time))), 13.0);
    float rim = pow(max(0.0, 1.0 - abs(r - 0.94) * 12.0), 2.0);
    half3 tint = half3(0.78, 0.88, 1.0);
    half3 color = scene.rgb * 0.72 + tint * half(0.13 + 0.035 * sweep);
    color += half3(1.0) * half(spec * 0.32 + rim * 0.23);
    color += half3(0.36, 0.64, 1.0) * half((1.0 - edge) * 0.055);
    return half4(color, half(0.72 + rim * 0.16));
}
""".trimIndent()

@Composable
fun LiquidGlassApp() {
    var selected by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            clock += 0.035f
            delay(32)
        }
    }
    val titles = listOf("Discover", "Explore", "Favorites", "Profile")
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF080F24), Color(0xFF172B55), Color(0xFF39275F), Color(0xFF10182D)))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF4AA8FF).copy(alpha=.75f), Color.Transparent), center=Offset(size.width*.18f,size.height*.25f), radius=size.width*.55f), radius=size.width*.55f, center=Offset(size.width*.18f,size.height*.25f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFB56BFF).copy(alpha=.55f), Color.Transparent), center=Offset(size.width*.85f,size.height*.62f), radius=size.width*.48f), radius=size.width*.48f, center=Offset(size.width*.85f,size.height*.62f))
            drawCircle(Color.White.copy(alpha=.10f), radius=size.width*.24f, center=Offset(size.width*.77f,size.height*.20f))
            drawCircle(Color(0xFF54D7FF).copy(alpha=.12f), radius=size.width*.18f, center=Offset(size.width*.25f,size.height*.72f))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=24.dp)) {
            Spacer(Modifier.height(28.dp))
            Text("LIQUID GLASS", color=Color.White.copy(alpha=.65f), fontSize=12.sp, letterSpacing=3.sp, fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Text(titles[selected], color=Color.White, fontSize=34.sp, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("A live optical glass experiment", color=Color.White.copy(alpha=.66f), fontSize=15.sp)
            Spacer(Modifier.height(30.dp))
            GlassInfoCard(clock)
            Spacer(Modifier.weight(1f))
            Text("REFRACTION  •  EDGE LIGHT  •  MOTION", color=Color.White.copy(alpha=.48f), fontSize=9.sp, letterSpacing=1.4.sp, modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=18.dp))
            LiquidTabBar(selected, clock, listOf("Home", "Explore", "Saved", "You"), listOf(Icons.Rounded.Home, Icons.Rounded.Explore, Icons.Rounded.Bookmark, Icons.Rounded.Person), { selected = it }, Modifier.fillMaxWidth().padding(bottom=18.dp))
        }
    }
}

@Composable
private fun GlassInfoCard(time: Float) {
    val pulse by animateFloatAsState(targetValue=1f + sin(time.toDouble()).toFloat()*.008f, animationSpec=tween(120), label="pulse")
    Box(Modifier.fillMaxWidth().height(210.dp).graphicsLayer { scaleX=pulse; scaleY=pulse }.clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Color.White.copy(.16f), Color.White.copy(.035f), Color(0xFF9CCBFF).copy(.10f))))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color.White.copy(alpha=.12f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()), style=Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(alpha=.12f), radius=65.dp.toPx(), center=Offset(size.width*.78f,size.height*.42f))
            drawCircle(Color(0xFF9AD7FF).copy(alpha=.13f), radius=44.dp.toPx(), center=Offset(size.width*.78f,size.height*.42f))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text("OPTICAL SURFACE", color=Color.White.copy(.65f), fontSize=10.sp, letterSpacing=2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Light bends.\nGlass comes alive.", color=Color.White, fontSize=23.sp, lineHeight=28.sp, fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LiquidTabBar(selected: Int, time: Float, labels: List<String>, icons: List<androidx.compose.ui.graphics.vector.ImageVector>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shader = remember { if (Build.VERSION.SDK_INT >= 33) RuntimeShader(glassShaderSource) else null }
    val sceneBitmap = remember {
        Bitmap.createBitmap(1200, 240, Bitmap.Config.ARGB_8888).also { bmp ->
            val c = NativeCanvas(bmp)
            c.drawColor(android.graphics.Color.rgb(30,50,90))
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.shader = RadialGradient(180f,60f,420f,intArrayOf(0xFF4FAAFF.toInt(),0x003C78CC),null,Shader.TileMode.CLAMP)
            c.drawRect(0f,0f,1200f,240f,p)
            p.shader = RadialGradient(920f,190f,430f,intArrayOf(0xFFB77AFF.toInt(),0x003C286A),null,Shader.TileMode.CLAMP)
            c.drawRect(0f,0f,1200f,240f,p)
            p.shader = null
            p.color = 0x22FFFFFF
            c.drawCircle(760f,70f,100f,p)
        }
    }
    Box(modifier.height(76.dp).clip(RoundedCornerShape(28.dp)).then(
        if (Build.VERSION.SDK_INT >= 33 && shader != null) Modifier.drawWithCache {
            val imageShader = ImageShader(sceneBitmap.asImageBitmap(), tileModeX=TileMode.Clamp, tileModeY=TileMode.Clamp)
            shader.setInputShader("backdrop", imageShader)
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("focus", size.width*.5f, size.height*.5f)
            shader.setFloatUniform("time", time)
            val brush = ShaderBrush(shader)
            onDrawBehind { drawRoundRect(brush, cornerRadius=androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())) }
        } else Modifier.background(Color.White.copy(alpha=.17f), RoundedCornerShape(28.dp))
    )) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color.White.copy(alpha=.26f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()), style=Stroke(1.dp.toPx()))
            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(.28f), Color.Transparent, Color.White.copy(.05f))), cornerRadius=androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()), style=Stroke(1.2.dp.toPx()))
        }
        Row(Modifier.fillMaxSize().padding(horizontal=7.dp, vertical=7.dp), verticalAlignment=Alignment.CenterVertically) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                val scale by animateFloatAsState(if (active) 1.045f else 1f, tween(300), label="iconScale$index")
                Column(Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX=scale; scaleY=scale }.clip(RoundedCornerShape(22.dp)).clickable { onSelect(index) }, horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                    Icon(icons[index], contentDescription=label, tint=if(active) Color.White else Color.White.copy(.65f), modifier=Modifier.size(22.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(label, color=if(active) Color.White else Color.White.copy(.65f), fontSize=10.sp, fontWeight=if(active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val x = (time*95f) % (size.width+100f)-50f
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(.36f),Color.Transparent),startX=x-45f,endX=x+45f),Offset(x,2.dp.toPx()),Offset(x+18f,2.dp.toPx()),strokeWidth=1.4.dp.toPx())
        }
    }
}
