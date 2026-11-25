package com.example.gamification.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.gamification.engine.AccessoryType
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarActionAnimation
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.AvatarMoodState
import com.example.gamification.engine.BodyBuild
import com.example.gamification.engine.BottomStyle
import com.example.gamification.engine.FacialHairStyle
import com.example.gamification.engine.HairStyle
import com.example.gamification.engine.SportStyle
import com.example.gamification.engine.TopStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AthleteAvatarCanvas(
    appearance: AthleteAppearance,
    moodState: AvatarMoodState,
    animation: AvatarActionAnimation,
    level: Int,
    backgroundTier: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    onAvatarTap: (() -> Unit)? = null,
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val accentOutline = MaterialTheme.colorScheme.outlineVariant

    val phase = if (reduceMotion) {
        0.25f
    } else {
        val infinite = rememberInfiniteTransition(label = "avatar_motion")
        val animPhase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "avatar_cycle",
        )
        animPhase
    }

    val breathe = if (reduceMotion) 0f else sin(phase * 2f * PI.toFloat())
    val blink = if (reduceMotion) false else (phase > 0.90f && phase < 0.95f)

    val boxModifier = if (onAvatarTap != null) {
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onAvatarTap,
            )
            .semantics {
                contentDescription =
                    "${appearance.animalSpecies.label} ${appearance.gender.label} ${appearance.name}, estado ${moodState.label}"
            }
    } else {
        modifier.semantics {
            contentDescription = "${appearance.animalSpecies.label} ${appearance.name}"
        }
    }

    Box(modifier = boxModifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas
            val cx = w * 0.5f
            val baseFloorY = h * 0.87f

            // 1. Background stage + sport stereotype environment
            drawStageBackdrop(
                w = w,
                h = h,
                cx = cx,
                floorY = baseFloorY,
                appearance = appearance,
                moodState = moodState,
                level = level,
                backgroundTier = backgroundTier,
                surfaceColor = surfaceColor,
                accentOutline = accentOutline,
                phase = phase,
            )

            // 2. Compute animation kinematics
            val verticalBob = when (animation) {
                AvatarActionAnimation.JOG_IN_PLACE -> -abs(breathe) * h * 0.035f
                AvatarActionAnimation.JUMP_ROPE -> -abs(sin(phase * 4f * PI.toFloat())) * h * 0.05f
                AvatarActionAnimation.CELEBRATE -> -abs(breathe) * h * 0.045f
                AvatarActionAnimation.SIT_REST -> h * 0.055f
                AvatarActionAnimation.SLEEP -> h * 0.07f
                else -> breathe * h * 0.012f
            }

            val buildScale = when (appearance.bodyBuild) {
                BodyBuild.AGILE -> 0.88f
                BodyBuild.ATHLETIC -> 1.0f
                BodyBuild.ROBUST -> 1.18f
            }
            val genderShoulderFactor = if (appearance.gender == AvatarGender.MALE) 1.07f else 0.93f
            val postureBoost = ((level - 1).coerceIn(0, 10)) * 0.002f
            val headR = minOf(w, h) * 0.145f
            val torsoH = h * (0.23f + postureBoost)
            val torsoW = headR * 1.48f * buildScale * genderShoulderFactor
            val hipY = baseFloorY - h * 0.21f + verticalBob
            val shoulderY = hipY - torsoH
            val headCy = shoulderY - headR * 1.16f

            // Floor shadow
            val shadowScale = (1f - abs(verticalBob) / (h * 0.18f).coerceAtLeast(1f)).coerceIn(0.65f, 1.05f)
            drawOval(
                color = Color.Black.copy(alpha = 0.14f),
                topLeft = Offset(cx - torsoW * 0.88f * shadowScale, baseFloorY - h * 0.018f),
                size = Size(torsoW * 1.76f * shadowScale, h * 0.042f),
            )

            // Sport & accessory floor props
            drawFloorProps(
                w = w,
                h = h,
                cx = cx,
                floorY = baseFloorY,
                appearance = appearance,
                animation = animation,
            )

            // Animal tail behind body
            drawAnimalTail(
                cx = cx,
                hipY = hipY,
                torsoW = torsoW,
                headR = headR,
                appearance = appearance,
                phase = phase,
                reduceMotion = reduceMotion,
            )

            // 3. Legs + Bottoms + Shoes
            drawLegsAndShoes(
                cx = cx,
                hipY = hipY,
                floorY = baseFloorY,
                torsoW = torsoW,
                headR = headR,
                appearance = appearance,
                animation = animation,
                phase = phase,
            )

            // 4. Arms + Hands/Paws + Wrist accessories + Props
            drawArmsAndProps(
                cx = cx,
                shoulderY = shoulderY,
                hipY = hipY,
                torsoW = torsoW,
                headR = headR,
                headCy = headCy,
                appearance = appearance,
                animation = animation,
                phase = phase,
                breathe = breathe,
            )

            // 5. Torso + Gym Top + Sport Stereotype Gear (belt / race bib / stripe) + Medal
            drawTorsoAndTop(
                cx = cx,
                shoulderY = shoulderY,
                hipY = hipY,
                torsoW = torsoW,
                headR = headR,
                appearance = appearance,
                level = level,
            )

            if (AccessoryType.TOWEL in appearance.accessories && animation != AvatarActionAnimation.TOWEL_WIPE) {
                drawShoulderTowel(cx, shoulderY, torsoW, appearance.secondaryColor)
            }

            // 6. Animal Head, Ears/Horns/Mane, Snout/Beak, Eyes, Hair, Headwear
            drawAnimalHeadAndFeatures(
                cx = cx,
                headCy = headCy,
                headR = headR,
                appearance = appearance,
                moodState = moodState,
                animation = animation,
                blink = blink,
            )

            // 7. Celebration sparkles or Sleep Zzz
            if (animation == AvatarActionAnimation.CELEBRATE || moodState == AvatarMoodState.CELEBRANDO_PR) {
                drawCelebrationSparkles(cx, headCy, headR, appearance.secondaryColor, phase)
            } else if (animation == AvatarActionAnimation.SLEEP || moodState == AvatarMoodState.DURMIENDO) {
                drawSleepBubbles(cx + headR * 1.15f, headCy - headR * 0.7f, headR, phase)
            }

            // Jump rope arc
            if (animation == AvatarActionAnimation.JUMP_ROPE) {
                val ropeDip = sin(phase * 4f * PI.toFloat())
                val ropePath = Path().apply {
                    moveTo(cx - torsoW * 1.15f, hipY + headR * 0.1f)
                    quadraticTo(
                        cx,
                        baseFloorY + ropeDip * h * 0.08f,
                        cx + torsoW * 1.15f,
                        hipY + headR * 0.1f,
                    )
                }
                drawPath(
                    path = ropePath,
                    color = appearance.secondaryColor,
                    style = Stroke(width = headR * 0.1f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun DrawScope.drawStageBackdrop(
    w: Float,
    h: Float,
    cx: Float,
    floorY: Float,
    appearance: AthleteAppearance,
    moodState: AvatarMoodState,
    level: Int,
    backgroundTier: Int,
    surfaceColor: Color,
    accentOutline: Color,
    phase: Float,
) {
    val moodTint = when (moodState) {
        AvatarMoodState.CELEBRANDO_PR, AvatarMoodState.CONTENTO_MEJORA -> appearance.secondaryColor.copy(alpha = 0.18f)
        AvatarMoodState.DURMIENDO -> Color(0xFF1E293B).copy(alpha = 0.22f)
        AvatarMoodState.INVITANDO_ACTIVIDAD -> Color(0xFFF59E0B).copy(alpha = 0.13f)
        else -> appearance.primaryColor.copy(alpha = 0.14f)
    }

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                moodTint,
                surfaceColor.copy(alpha = 0.35f),
                surfaceColor.copy(alpha = 0.65f),
            )
        ),
        cornerRadius = CornerRadius(24f, 24f),
    )

    // Sport stereotype backdrop silhouette (pull-up bar for calisthenics, rack for gym/strength, track lines for running)
    if (appearance.sportStyle == SportStyle.CALISTHENICS || backgroundTier >= 2) {
        val rackAlpha = 0.12f
        drawLine(
            color = accentOutline.copy(alpha = rackAlpha),
            start = Offset(w * 0.18f, h * 0.14f),
            end = Offset(w * 0.18f, floorY),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accentOutline.copy(alpha = rackAlpha),
            start = Offset(w * 0.82f, h * 0.14f),
            end = Offset(w * 0.82f, floorY),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accentOutline.copy(alpha = rackAlpha),
            start = Offset(w * 0.15f, h * 0.16f),
            end = Offset(w * 0.85f, h * 0.16f),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
    }

    if (appearance.sportStyle == SportStyle.RUNNING) {
        for (i in 0..2) {
            val y = floorY - h * (0.01f + i * 0.028f)
            drawLine(
                color = appearance.secondaryColor.copy(alpha = 0.16f),
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
    }

    if (level >= 5 || backgroundTier >= 3) {
        val pulse = 0.92f + 0.08f * sin(phase * 2f * PI.toFloat())
        val auraRadius = (minOf(w, h) * 0.42f * pulse).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    appearance.secondaryColor.copy(alpha = 0.20f),
                    Color.Transparent,
                ),
                center = Offset(cx, h * 0.45f),
                radius = auraRadius,
            ),
            center = Offset(cx, h * 0.45f),
            radius = auraRadius,
        )
    }

    drawOval(
        color = appearance.primaryColor.copy(alpha = 0.11f),
        topLeft = Offset(cx - w * 0.34f, floorY - h * 0.03f),
        size = Size(w * 0.68f, h * 0.09f),
    )
}

private fun DrawScope.drawFloorProps(
    w: Float,
    h: Float,
    cx: Float,
    floorY: Float,
    appearance: AthleteAppearance,
    animation: AvatarActionAnimation,
) {
    if (AccessoryType.BOTTLE in appearance.accessories && animation != AvatarActionAnimation.DRINK_WATER) {
        val bx = cx + w * 0.28f
        val by = floorY - h * 0.11f
        val bw = w * 0.055f
        val bh = h * 0.105f
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(bx, by),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(bw * 0.35f, bw * 0.35f),
        )
        drawRoundRect(
            color = Color(0xFF0284C7),
            topLeft = Offset(bx + bw * 0.22f, by - bh * 0.18f),
            size = Size(bw * 0.56f, bh * 0.2f),
            cornerRadius = CornerRadius(4f, 4f),
        )
    }

    val propX = cx - w * 0.29f
    val r = minOf(w, h) * 0.048f
    when (appearance.sportStyle) {
        SportStyle.SOCCER -> {
            val ballCenter = Offset(propX, floorY - r * 0.85f)
            drawCircle(color = Color.White, radius = r, center = ballCenter)
            drawCircle(color = Color(0xFF1E293B), radius = r, center = ballCenter, style = Stroke(2.5f))
            drawCircle(color = Color(0xFF1E293B), radius = r * 0.34f, center = ballCenter)
        }
        SportStyle.CYCLING -> {
            val wheelCenter = Offset(propX, floorY - r * 1.05f)
            drawCircle(color = Color(0xFF334155), radius = r * 1.1f, center = wheelCenter, style = Stroke(4f))
            drawCircle(color = appearance.secondaryColor, radius = r * 0.30f, center = wheelCenter)
            drawLine(color = Color(0xFF64748B), start = Offset(wheelCenter.x - r, wheelCenter.y), end = Offset(wheelCenter.x + r, wheelCenter.y), strokeWidth = 2f)
            drawLine(color = Color(0xFF64748B), start = Offset(wheelCenter.x, wheelCenter.y - r), end = Offset(wheelCenter.x, wheelCenter.y + r), strokeWidth = 2f)
        }
        SportStyle.STRENGTH, SportStyle.POWERLIFTING, SportStyle.CROSS_TRAINING -> {
            val ky = floorY - h * 0.045f
            val kr = minOf(w, h) * 0.042f
            drawCircle(
                color = Color(0xFF334155),
                radius = kr * 0.65f,
                center = Offset(propX, ky - kr * 0.85f),
                style = Stroke(width = kr * 0.28f),
            )
            drawCircle(color = Color(0xFF1E293B), radius = kr, center = Offset(propX, ky))
        }
        SportStyle.HYPERTROPHY -> {
            // Mini floor dumbbell
            val dy = floorY - h * 0.03f
            drawLine(Color(0xFF64748B), Offset(propX - r, dy), Offset(propX + r, dy), strokeWidth = 5f, cap = StrokeCap.Round)
            drawRoundRect(Color(0xFF1E293B), Offset(propX - r - 6f, dy - 10f), Size(10f, 20f), CornerRadius(3f, 3f))
            drawRoundRect(Color(0xFF1E293B), Offset(propX + r - 4f, dy - 10f), Size(10f, 20f), CornerRadius(3f, 3f))
        }
        SportStyle.GENERAL_FITNESS, SportStyle.FUNCTIONAL -> {
            // Agility cone
            val cone = Path().apply {
                moveTo(propX, floorY - r * 1.7f)
                lineTo(propX - r * 0.7f, floorY)
                lineTo(propX + r * 0.7f, floorY)
                close()
            }
            drawPath(cone, color = Color(0xFFF97316))
        }
        else -> Unit
    }
}

private fun DrawScope.drawAnimalTail(
    cx: Float,
    hipY: Float,
    torsoW: Float,
    headR: Float,
    appearance: AthleteAppearance,
    phase: Float,
    reduceMotion: Boolean,
) {
    val wag = if (reduceMotion) 0f else sin(phase * 4f * PI.toFloat()) * headR * 0.22f
    val base = Offset(cx - torsoW * 0.42f, hipY - headR * 0.1f)
    when (appearance.animalSpecies) {
        AnimalSpecies.LION -> {
            val tip = Offset(cx - torsoW * 1.05f, hipY - headR * 0.85f + wag)
            val tailPath = Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(cx - torsoW * 0.95f, hipY + headR * 0.1f, tip.x, tip.y)
            }
            drawPath(tailPath, color = appearance.skinColor, style = Stroke(headR * 0.14f, cap = StrokeCap.Round))
            drawCircle(color = appearance.hairColor, radius = headR * 0.20f, center = tip)
        }
        AnimalSpecies.TIGER -> {
            val tip = Offset(cx - torsoW * 1.08f, hipY - headR * 0.88f + wag)
            val tailPath = Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(cx - torsoW * 0.96f, hipY + headR * 0.12f, tip.x, tip.y)
            }
            drawPath(tailPath, color = appearance.skinColor, style = Stroke(headR * 0.18f, cap = StrokeCap.Round))
            drawCircle(color = Color(0xFF1F2937), radius = headR * 0.12f, center = tip)
        }
        AnimalSpecies.WOLF, AnimalSpecies.FOX -> {
            val tip = Offset(cx - torsoW * 1.12f, hipY - headR * 0.65f + wag)
            val tailPath = Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(cx - torsoW * 0.9f, hipY + headR * 0.05f, tip.x, tip.y)
            }
            drawPath(tailPath, color = appearance.skinColor, style = Stroke(headR * 0.36f, cap = StrokeCap.Round))
            if (appearance.animalSpecies == AnimalSpecies.FOX) {
                drawCircle(color = Color(0xFFFDF8F2), radius = headR * 0.18f, center = tip)
            }
        }
        AnimalSpecies.PANTHER -> {
            val tip = Offset(cx - torsoW * 1.10f, hipY - headR * 0.95f + wag)
            val tailPath = Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(cx - torsoW * 1.0f, hipY + headR * 0.18f, tip.x, tip.y)
            }
            drawPath(tailPath, color = appearance.skinColor, style = Stroke(headR * 0.16f, cap = StrokeCap.Round))
        }
        AnimalSpecies.BEAR -> {
            drawCircle(color = appearance.skinColor, radius = headR * 0.20f, center = Offset(base.x - headR * 0.14f, base.y))
        }
        AnimalSpecies.BULL -> {
            val tip = Offset(cx - torsoW * 0.95f, hipY + headR * 0.15f + wag * 0.5f)
            drawLine(appearance.skinColor, base, tip, strokeWidth = headR * 0.11f, cap = StrokeCap.Round)
            drawCircle(color = appearance.hairColor, radius = headR * 0.14f, center = tip)
        }
        AnimalSpecies.EAGLE -> {
            // Short tail feathers
            drawOval(
                color = appearance.skinColor.copy(alpha = 0.85f),
                topLeft = Offset(base.x - headR * 0.45f, base.y - headR * 0.12f),
                size = Size(headR * 0.55f, headR * 0.28f),
            )
        }
        AnimalSpecies.GORILLA -> Unit
    }
}

private fun DrawScope.drawLegsAndShoes(
    cx: Float,
    hipY: Float,
    floorY: Float,
    torsoW: Float,
    headR: Float,
    appearance: AthleteAppearance,
    animation: AvatarActionAnimation,
    phase: Float,
) {
    val legStroke = headR * 0.36f
    val stanceHalf = torsoW * 0.34f
    val kneeOffsetL = if (animation == AvatarActionAnimation.JOG_IN_PLACE) {
        sin(phase * 4f * PI.toFloat()) * headR * 0.25f
    } else 0f
    val kneeOffsetR = -kneeOffsetL

    val leftFoot = Offset(cx - stanceHalf * 1.12f, floorY - headR * 0.14f + kneeOffsetL.coerceAtMost(0f))
    val rightFoot = Offset(cx + stanceHalf * 1.12f, floorY - headR * 0.14f + kneeOffsetR.coerceAtMost(0f))
    val leftHip = Offset(cx - stanceHalf * 0.72f, hipY)
    val rightHip = Offset(cx + stanceHalf * 0.72f, hipY)

    val legColor = if (appearance.animalSpecies == AnimalSpecies.EAGLE) Color(0xFFF59E0B) else appearance.skinColor
    drawLine(legColor, leftHip, leftFoot, strokeWidth = legStroke, cap = StrokeCap.Round)
    drawLine(legColor, rightHip, rightFoot, strokeWidth = legStroke, cap = StrokeCap.Round)

    val bottomColor = appearance.secondaryColor
    when (appearance.bottomStyle) {
        BottomStyle.SHORTS -> {
            val midL = Offset(
                leftHip.x + (leftFoot.x - leftHip.x) * 0.48f,
                leftHip.y + (leftFoot.y - leftHip.y) * 0.48f,
            )
            val midR = Offset(
                rightHip.x + (rightFoot.x - rightHip.x) * 0.48f,
                rightHip.y + (rightFoot.y - rightHip.y) * 0.48f,
            )
            drawLine(bottomColor, leftHip, midL, strokeWidth = legStroke * 1.24f, cap = StrokeCap.Round)
            drawLine(bottomColor, rightHip, midR, strokeWidth = legStroke * 1.24f, cap = StrokeCap.Round)
            drawRoundRect(
                color = bottomColor,
                topLeft = Offset(cx - torsoW * 0.46f, hipY - headR * 0.2f),
                size = Size(torsoW * 0.92f, headR * 0.42f),
                cornerRadius = CornerRadius(12f, 12f),
            )
        }
        BottomStyle.LEGGINGS -> {
            drawLine(bottomColor, leftHip, leftFoot, strokeWidth = legStroke * 1.08f, cap = StrokeCap.Round)
            drawLine(bottomColor, rightHip, rightFoot, strokeWidth = legStroke * 1.08f, cap = StrokeCap.Round)
            drawRoundRect(
                color = bottomColor,
                topLeft = Offset(cx - torsoW * 0.44f, hipY - headR * 0.2f),
                size = Size(torsoW * 0.88f, headR * 0.36f),
                cornerRadius = CornerRadius(10f, 10f),
            )
        }
        BottomStyle.JOGGERS -> {
            drawLine(bottomColor, leftHip, leftFoot, strokeWidth = legStroke * 1.32f, cap = StrokeCap.Round)
            drawLine(bottomColor, rightHip, rightFoot, strokeWidth = legStroke * 1.32f, cap = StrokeCap.Round)
            drawRoundRect(
                color = bottomColor,
                topLeft = Offset(cx - torsoW * 0.48f, hipY - headR * 0.2f),
                size = Size(torsoW * 0.96f, headR * 0.44f),
                cornerRadius = CornerRadius(14f, 14f),
            )
        }
    }

    val shoeW = headR * 0.62f
    val shoeH = headR * 0.30f
    drawRoundRect(
        color = appearance.shoeColor,
        topLeft = Offset(leftFoot.x - shoeW * 0.62f, leftFoot.y - shoeH * 0.35f),
        size = Size(shoeW, shoeH),
        cornerRadius = CornerRadius(shoeH * 0.45f, shoeH * 0.45f),
    )
    drawRoundRect(
        color = appearance.shoeColor,
        topLeft = Offset(rightFoot.x - shoeW * 0.38f, rightFoot.y - shoeH * 0.35f),
        size = Size(shoeW, shoeH),
        cornerRadius = CornerRadius(shoeH * 0.45f, shoeH * 0.45f),
    )
    drawLine(
        color = Color.White,
        start = Offset(leftFoot.x - shoeW * 0.56f, leftFoot.y + shoeH * 0.56f),
        end = Offset(leftFoot.x + shoeW * 0.34f, leftFoot.y + shoeH * 0.56f),
        strokeWidth = shoeH * 0.2f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White,
        start = Offset(rightFoot.x - shoeW * 0.34f, rightFoot.y + shoeH * 0.56f),
        end = Offset(rightFoot.x + shoeW * 0.56f, rightFoot.y + shoeH * 0.56f),
        strokeWidth = shoeH * 0.2f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawArmsAndProps(
    cx: Float,
    shoulderY: Float,
    hipY: Float,
    torsoW: Float,
    headR: Float,
    headCy: Float,
    appearance: AthleteAppearance,
    animation: AvatarActionAnimation,
    phase: Float,
    breathe: Float,
) {
    val armStroke = headR * 0.30f
    val leftShoulder = Offset(cx - torsoW * 0.46f, shoulderY + headR * 0.12f)
    val rightShoulder = Offset(cx + torsoW * 0.46f, shoulderY + headR * 0.12f)

    val liftCycle = (sin(phase * 2f * PI.toFloat()) + 1f) * 0.5f

    val (leftHand, rightHand) = when (animation) {
        AvatarActionAnimation.DUMBBELL_LIFT -> {
            val curlY = shoulderY + headR * (0.65f - 0.62f * liftCycle)
            Offset(cx - torsoW * 0.88f, curlY) to Offset(cx + torsoW * 0.88f, curlY)
        }
        AvatarActionAnimation.CELEBRATE -> {
            val cheerY = headCy - headR * (0.45f + 0.18f * abs(breathe))
            Offset(cx - torsoW * 0.92f, cheerY) to Offset(cx + torsoW * 0.92f, cheerY)
        }
        AvatarActionAnimation.STRETCH -> {
            val stretchY = headCy - headR * 0.55f
            Offset(cx - torsoW * 0.45f, stretchY) to Offset(cx + torsoW * 0.45f, stretchY)
        }
        AvatarActionAnimation.DRINK_WATER -> {
            Offset(cx - torsoW * 0.78f, hipY - headR * 0.05f) to
                Offset(cx + headR * 0.55f, headCy + headR * 0.42f)
        }
        AvatarActionAnimation.TOWEL_WIPE -> {
            Offset(cx - torsoW * 0.78f, hipY - headR * 0.05f) to
                Offset(cx + headR * 0.58f, headCy - headR * 0.15f)
        }
        AvatarActionAnimation.JUMP_ROPE -> {
            Offset(cx - torsoW * 1.08f, hipY + headR * 0.05f) to
                Offset(cx + torsoW * 1.08f, hipY + headR * 0.05f)
        }
        AvatarActionAnimation.JOG_IN_PLACE -> {
            val swing = sin(phase * 4f * PI.toFloat()) * headR * 0.42f
            Offset(cx - torsoW * 0.78f, shoulderY + headR * 0.85f + swing) to
                Offset(cx + torsoW * 0.78f, shoulderY + headR * 0.85f - swing)
        }
        else -> {
            val sway = breathe * headR * 0.08f
            Offset(cx - torsoW * 0.82f, hipY - headR * 0.05f + sway) to
                Offset(cx + torsoW * 0.82f, hipY - headR * 0.05f - sway)
        }
    }

    drawLine(appearance.skinColor, leftShoulder, leftHand, strokeWidth = armStroke, cap = StrokeCap.Round)
    drawLine(appearance.skinColor, rightShoulder, rightHand, strokeWidth = armStroke, cap = StrokeCap.Round)

    if (appearance.topStyle != TopStyle.TANK_TOP) {
        val sleeveRatio = if (appearance.topStyle == TopStyle.HOODIE) 0.72f else 0.36f
        val leftSleeveEnd = Offset(
            leftShoulder.x + (leftHand.x - leftShoulder.x) * sleeveRatio,
            leftShoulder.y + (leftHand.y - leftShoulder.y) * sleeveRatio,
        )
        val rightSleeveEnd = Offset(
            rightShoulder.x + (rightHand.x - rightShoulder.x) * sleeveRatio,
            rightShoulder.y + (rightHand.y - rightShoulder.y) * sleeveRatio,
        )
        drawLine(appearance.primaryColor, leftShoulder, leftSleeveEnd, strokeWidth = armStroke * 1.24f, cap = StrokeCap.Round)
        drawLine(appearance.primaryColor, rightShoulder, rightSleeveEnd, strokeWidth = armStroke * 1.24f, cap = StrokeCap.Round)
    }

    if (AccessoryType.WRISTBANDS in appearance.accessories) {
        val lw = Offset(
            leftShoulder.x + (leftHand.x - leftShoulder.x) * 0.80f,
            leftShoulder.y + (leftHand.y - leftShoulder.y) * 0.80f,
        )
        val rw = Offset(
            rightShoulder.x + (rightHand.x - rightShoulder.x) * 0.80f,
            rightShoulder.y + (rightHand.y - rightShoulder.y) * 0.80f,
        )
        drawCircle(appearance.secondaryColor, radius = armStroke * 0.62f, center = lw)
        drawCircle(appearance.secondaryColor, radius = armStroke * 0.62f, center = rw)
    }

    if (AccessoryType.WATCH in appearance.accessories) {
        val watchPos = Offset(
            leftShoulder.x + (leftHand.x - leftShoulder.x) * 0.84f,
            leftShoulder.y + (leftHand.y - leftShoulder.y) * 0.84f,
        )
        drawCircle(Color(0xFF0F172A), radius = armStroke * 0.56f, center = watchPos)
        drawCircle(Color(0xFF38BDF8), radius = armStroke * 0.30f, center = watchPos)
    }

    val handColor = when {
        AccessoryType.GLOVES in appearance.accessories -> Color(0xFF1E293B)
        appearance.animalSpecies == AnimalSpecies.BULL -> Color(0xFF1E293B) // Hooves!
        else -> appearance.skinColor
    }
    drawCircle(handColor, radius = armStroke * 0.58f, center = leftHand)
    drawCircle(handColor, radius = armStroke * 0.58f, center = rightHand)

    when (animation) {
        AvatarActionAnimation.DUMBBELL_LIFT -> {
            drawDumbbellInHand(leftHand, headR)
            drawDumbbellInHand(rightHand, headR)
        }
        AvatarActionAnimation.DRINK_WATER -> {
            drawRoundRect(
                color = Color(0xFF38BDF8),
                topLeft = Offset(rightHand.x - headR * 0.14f, rightHand.y - headR * 0.48f),
                size = Size(headR * 0.28f, headR * 0.56f),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
        AvatarActionAnimation.TOWEL_WIPE -> {
            drawRoundRect(
                color = appearance.secondaryColor,
                topLeft = Offset(rightHand.x - headR * 0.24f, rightHand.y - headR * 0.18f),
                size = Size(headR * 0.46f, headR * 0.34f),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
        else -> Unit
    }
}

private fun DrawScope.drawDumbbellInHand(hand: Offset, headR: Float) {
    val barHalf = headR * 0.38f
    drawLine(
        color = Color(0xFF64748B),
        start = Offset(hand.x - barHalf, hand.y),
        end = Offset(hand.x + barHalf, hand.y),
        strokeWidth = headR * 0.11f,
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(hand.x - barHalf - headR * 0.10f, hand.y - headR * 0.22f),
        size = Size(headR * 0.18f, headR * 0.44f),
        cornerRadius = CornerRadius(4f, 4f),
    )
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(hand.x + barHalf - headR * 0.08f, hand.y - headR * 0.22f),
        size = Size(headR * 0.18f, headR * 0.44f),
        cornerRadius = CornerRadius(4f, 4f),
    )
}

private fun DrawScope.drawTorsoAndTop(
    cx: Float,
    shoulderY: Float,
    hipY: Float,
    torsoW: Float,
    headR: Float,
    appearance: AthleteAppearance,
    level: Int,
) {
    val torsoH = (hipY - shoulderY).coerceAtLeast(headR)
    // Neck
    drawRoundRect(
        color = appearance.skinColor,
        topLeft = Offset(cx - headR * 0.24f, shoulderY - headR * 0.22f),
        size = Size(headR * 0.48f, headR * 0.38f),
        cornerRadius = CornerRadius(8f, 8f),
    )

    // Gender-styled torso silhouette (straight broad for Hombre, gently tapered waist for Mujer)
    val waistHalf = if (appearance.gender == AvatarGender.FEMALE) torsoW * 0.39f else torsoW * 0.48f
    val torsoPath = Path().apply {
        moveTo(cx - torsoW * 0.46f, shoulderY)
        lineTo(cx + torsoW * 0.46f, shoulderY)
        quadraticTo(cx + waistHalf * 0.96f, shoulderY + torsoH * 0.55f, cx + torsoW * 0.46f, hipY)
        lineTo(cx - torsoW * 0.46f, hipY)
        quadraticTo(cx - waistHalf * 0.96f, shoulderY + torsoH * 0.55f, cx - torsoW * 0.46f, shoulderY)
        close()
    }
    drawPath(
        path = torsoPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                appearance.primaryColor,
                appearance.primaryColor.copy(alpha = 0.88f),
            )
        ),
    )

    when (appearance.topStyle) {
        TopStyle.TANK_TOP -> {
            val neckPath = Path().apply {
                moveTo(cx - headR * 0.34f, shoulderY)
                lineTo(cx, shoulderY + headR * (if (appearance.gender == AvatarGender.FEMALE) 0.46f else 0.38f))
                lineTo(cx + headR * 0.34f, shoulderY)
                close()
            }
            drawPath(neckPath, color = appearance.skinColor)
        }
        TopStyle.HOODIE -> {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(cx - headR * 0.16f, shoulderY + headR * 0.12f),
                end = Offset(cx - headR * 0.16f, shoulderY + headR * 0.62f),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(cx + headR * 0.16f, shoulderY + headR * 0.12f),
                end = Offset(cx + headR * 0.16f, shoulderY + headR * 0.62f),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
        TopStyle.JERSEY -> {
            drawRoundRect(
                color = appearance.secondaryColor,
                topLeft = Offset(cx - torsoW * 0.40f, shoulderY + torsoH * 0.08f),
                size = Size(torsoW * 0.13f, torsoH * 0.82f),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
        TopStyle.TSHIRT -> {
            drawCircle(
                color = appearance.secondaryColor,
                radius = headR * 0.22f,
                center = Offset(cx, shoulderY + headR * 0.06f),
                style = Stroke(width = 4f),
            )
        }
    }

    // Sport stereotype gear on torso
    when (appearance.sportStyle) {
        SportStyle.POWERLIFTING, SportStyle.STRENGTH -> {
            // Lifting belt
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - torsoW * 0.47f, hipY - headR * 0.34f),
                size = Size(torsoW * 0.94f, headR * 0.24f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = Color(0xFFCBD5E1),
                topLeft = Offset(cx - headR * 0.14f, hipY - headR * 0.33f),
                size = Size(headR * 0.28f, headR * 0.22f),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
        SportStyle.RUNNING -> {
            // Marathon / Runner bib patch on chest
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(cx - torsoW * 0.22f, shoulderY + torsoH * 0.32f),
                size = Size(torsoW * 0.44f, torsoH * 0.32f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawLine(
                color = appearance.secondaryColor,
                start = Offset(cx - torsoW * 0.14f, shoulderY + torsoH * 0.48f),
                end = Offset(cx + torsoW * 0.14f, shoulderY + torsoH * 0.48f),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
        else -> Unit
    }

    if (level >= 3) {
        val medalCenter = Offset(cx + torsoW * 0.23f, shoulderY + torsoH * 0.28f)
        drawCircle(color = Color(0xFFFACC15), radius = headR * 0.13f, center = medalCenter)
        drawCircle(color = Color(0xFFCA8A04), radius = headR * 0.07f, center = medalCenter)
    }
}

private fun DrawScope.drawShoulderTowel(
    cx: Float,
    shoulderY: Float,
    torsoW: Float,
    towelColor: Color,
) {
    drawRoundRect(
        color = towelColor,
        topLeft = Offset(cx - torsoW * 0.50f, shoulderY - torsoW * 0.05f),
        size = Size(torsoW * 0.24f, torsoW * 0.56f),
        cornerRadius = CornerRadius(10f, 10f),
    )
}

private fun DrawScope.drawAnimalHeadAndFeatures(
    cx: Float,
    headCy: Float,
    headR: Float,
    appearance: AthleteAppearance,
    moodState: AvatarMoodState,
    animation: AvatarActionAnimation,
    blink: Boolean,
) {
    val furColor = appearance.skinColor
    val lightPatch = Color(0xFFF7EBDD)

    // 1. Hair behind head if Long or Ponytail
    if (appearance.hairStyle == HairStyle.LONG_FLOW) {
        drawRoundRect(
            color = appearance.hairColor,
            topLeft = Offset(cx - headR * 1.12f, headCy - headR * 0.4f),
            size = Size(headR * 2.24f, headR * 1.65f),
            cornerRadius = CornerRadius(headR * 0.8f, headR * 0.8f),
        )
    } else if (appearance.hairStyle == HairStyle.PONYTAIL) {
        rotate(degrees = 15f, pivot = Offset(cx + headR * 0.9f, headCy - headR * 0.4f)) {
            drawOval(
                color = appearance.hairColor,
                topLeft = Offset(cx + headR * 0.72f, headCy - headR * 0.45f),
                size = Size(headR * 0.65f, headR * 1.15f),
            )
        }
    }

    // 2. Lion mane behind head (fuller for Male lion, sleeker or styled for Female lioness)
    if (appearance.animalSpecies == AnimalSpecies.LION) {
        val maneR = if (appearance.gender == AvatarGender.MALE) headR * 1.32f else headR * 1.14f
        drawCircle(color = appearance.hairColor, radius = maneR, center = Offset(cx, headCy))
    }

    // 3. Species-specific ears & horns behind/on head
    drawAnimalEarsAndHorns(cx, headCy, headR, appearance)

    // 4. Head base (rounded Pou-style animal silhouette)
    drawCircle(
        color = furColor,
        radius = headR,
        center = Offset(cx, headCy),
    )

    // Fox cheek tufts & Tiger stripes
    if (appearance.animalSpecies == AnimalSpecies.FOX) {
        val leftTuft = Path().apply {
            moveTo(cx - headR * 0.88f, headCy + headR * 0.05f)
            lineTo(cx - headR * 1.16f, headCy + headR * 0.28f)
            lineTo(cx - headR * 0.75f, headCy + headR * 0.46f)
            close()
        }
        val rightTuft = Path().apply {
            moveTo(cx + headR * 0.88f, headCy + headR * 0.05f)
            lineTo(cx + headR * 1.16f, headCy + headR * 0.28f)
            lineTo(cx + headR * 0.75f, headCy + headR * 0.46f)
            close()
        }
        drawPath(leftTuft, color = lightPatch)
        drawPath(rightTuft, color = lightPatch)
    } else if (appearance.animalSpecies == AnimalSpecies.TIGER) {
        val stripeColor = Color(0xFF1F2937)
        // Forehead stripes
        drawLine(stripeColor, Offset(cx, headCy - headR * 0.85f), Offset(cx, headCy - headR * 0.46f), strokeWidth = headR * 0.09f, cap = StrokeCap.Round)
        drawLine(stripeColor, Offset(cx - headR * 0.22f, headCy - headR * 0.78f), Offset(cx - headR * 0.14f, headCy - headR * 0.48f), strokeWidth = headR * 0.075f, cap = StrokeCap.Round)
        drawLine(stripeColor, Offset(cx + headR * 0.22f, headCy - headR * 0.78f), Offset(cx + headR * 0.14f, headCy - headR * 0.48f), strokeWidth = headR * 0.075f, cap = StrokeCap.Round)
        // Cheek stripes
        drawLine(stripeColor, Offset(cx - headR * 0.94f, headCy - headR * 0.04f), Offset(cx - headR * 0.62f, headCy + headR * 0.02f), strokeWidth = headR * 0.08f, cap = StrokeCap.Round)
        drawLine(stripeColor, Offset(cx - headR * 0.90f, headCy + headR * 0.18f), Offset(cx - headR * 0.60f, headCy + headR * 0.18f), strokeWidth = headR * 0.075f, cap = StrokeCap.Round)
        drawLine(stripeColor, Offset(cx + headR * 0.94f, headCy - headR * 0.04f), Offset(cx + headR * 0.62f, headCy + headR * 0.02f), strokeWidth = headR * 0.08f, cap = StrokeCap.Round)
        drawLine(stripeColor, Offset(cx + headR * 0.90f, headCy + headR * 0.18f), Offset(cx + headR * 0.60f, headCy + headR * 0.18f), strokeWidth = headR * 0.075f, cap = StrokeCap.Round)
    }

    // Gorilla / Eagle facial mask contrast
    if (appearance.animalSpecies == AnimalSpecies.GORILLA) {
        drawOval(
            color = Color(0xFFD6B89C),
            topLeft = Offset(cx - headR * 0.76f, headCy - headR * 0.35f),
            size = Size(headR * 1.52f, headR * 1.25f),
        )
    }

    // Exertion blush
    if (moodState == AvatarMoodState.ENTRENANDO ||
        moodState == AvatarMoodState.CELEBRANDO_PR ||
        moodState == AvatarMoodState.CANSADO
    ) {
        drawCircle(
            color = Color(0xFFFB7185).copy(alpha = 0.28f),
            radius = headR * 0.16f,
            center = Offset(cx - headR * 0.56f, headCy + headR * 0.15f),
        )
        drawCircle(
            color = Color(0xFFFB7185).copy(alpha = 0.28f),
            radius = headR * 0.16f,
            center = Offset(cx + headR * 0.56f, headCy + headR * 0.15f),
        )
    }

    // 5. Eyes + female eyelashes + brows
    val eyeOffsetX = headR * 0.36f
    val eyeY = headCy - headR * 0.10f
    val isSleeping = animation == AvatarActionAnimation.SLEEP || moodState == AvatarMoodState.DURMIENDO

    if (isSleeping || blink) {
        drawLine(
            color = Color(0xFF1F2937),
            start = Offset(cx - eyeOffsetX - headR * 0.14f, eyeY),
            end = Offset(cx - eyeOffsetX + headR * 0.14f, eyeY),
            strokeWidth = headR * 0.08f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF1F2937),
            start = Offset(cx + eyeOffsetX - headR * 0.14f, eyeY),
            end = Offset(cx + eyeOffsetX + headR * 0.14f, eyeY),
            strokeWidth = headR * 0.08f,
            cap = StrokeCap.Round,
        )
    } else {
        val eyeR = headR * 0.18f
        drawCircle(Color.White, radius = eyeR, center = Offset(cx - eyeOffsetX, eyeY))
        drawCircle(Color.White, radius = eyeR, center = Offset(cx + eyeOffsetX, eyeY))
        drawCircle(appearance.eyeColor, radius = eyeR * 0.66f, center = Offset(cx - eyeOffsetX, eyeY))
        drawCircle(appearance.eyeColor, radius = eyeR * 0.66f, center = Offset(cx + eyeOffsetX, eyeY))
        drawCircle(Color.White, radius = eyeR * 0.24f, center = Offset(cx - eyeOffsetX - eyeR * 0.22f, eyeY - eyeR * 0.22f))
        drawCircle(Color.White, radius = eyeR * 0.24f, center = Offset(cx + eyeOffsetX - eyeR * 0.22f, eyeY - eyeR * 0.22f))

        // Eyelashes when Female is chosen
        if (appearance.gender == AvatarGender.FEMALE) {
            drawLine(
                color = Color(0xFF1F2937),
                start = Offset(cx - eyeOffsetX - eyeR * 0.7f, eyeY - eyeR * 0.55f),
                end = Offset(cx - eyeOffsetX - eyeR * 1.15f, eyeY - eyeR * 0.95f),
                strokeWidth = headR * 0.055f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF1F2937),
                start = Offset(cx + eyeOffsetX + eyeR * 0.7f, eyeY - eyeR * 0.55f),
                end = Offset(cx + eyeOffsetX + eyeR * 1.15f, eyeY - eyeR * 0.95f),
                strokeWidth = headR * 0.055f,
                cap = StrokeCap.Round,
            )
        }
    }

    val browTilt = when (moodState) {
        AvatarMoodState.ENTRENANDO, AvatarMoodState.MOTIVADO -> headR * 0.07f
        AvatarMoodState.CANSADO, AvatarMoodState.INVITANDO_ACTIVIDAD -> -headR * 0.04f
        else -> 0f
    }
    drawLine(
        color = appearance.hairColor,
        start = Offset(cx - eyeOffsetX - headR * 0.16f, eyeY - headR * 0.28f - browTilt),
        end = Offset(cx - eyeOffsetX + headR * 0.16f, eyeY - headR * 0.28f + browTilt),
        strokeWidth = if (appearance.gender == AvatarGender.FEMALE) headR * 0.055f else headR * 0.08f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = appearance.hairColor,
        start = Offset(cx + eyeOffsetX - headR * 0.16f, eyeY - headR * 0.28f + browTilt),
        end = Offset(cx + eyeOffsetX + headR * 0.16f, eyeY - headR * 0.28f - browTilt),
        strokeWidth = if (appearance.gender == AvatarGender.FEMALE) headR * 0.055f else headR * 0.08f,
        cap = StrokeCap.Round,
    )

    // 6. Snout / Muzzle / Beak + Facial Hair + Mouth
    drawAnimalMuzzleAndMouth(cx, headCy, headR, appearance, moodState, lightPatch)

    // 7. Hair tuft / crest on top (respecting Eagle feathers & Lion mane)
    drawHairTop(cx, headCy, headR, appearance)

    // 8. Headband or Cap
    if (AccessoryType.HEADBAND in appearance.accessories) {
        drawRoundRect(
            color = appearance.secondaryColor,
            topLeft = Offset(cx - headR * 0.99f, headCy - headR * 0.58f),
            size = Size(headR * 1.98f, headR * 0.22f),
            cornerRadius = CornerRadius(10f, 10f),
        )
    }
    if (AccessoryType.CAP in appearance.accessories) {
        drawArc(
            color = appearance.primaryColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - headR * 1.03f, headCy - headR * 1.08f),
            size = Size(headR * 2.06f, headR * 1.15f),
        )
        drawRoundRect(
            color = appearance.secondaryColor,
            topLeft = Offset(cx - headR * 0.2f, headCy - headR * 0.56f),
            size = Size(headR * 1.48f, headR * 0.20f),
            cornerRadius = CornerRadius(12f, 12f),
        )
    }
}

private fun DrawScope.drawAnimalEarsAndHorns(
    cx: Float,
    headCy: Float,
    headR: Float,
    appearance: AthleteAppearance,
) {
    val fur = appearance.skinColor
    val innerPink = Color(0xFFF3C6C6)
    val hornColor = Color(0xFFF1E7D2)

    when (appearance.animalSpecies) {
        AnimalSpecies.BULL -> {
            // Side ears
            drawOval(fur, Offset(cx - headR * 1.24f, headCy - headR * 0.35f), Size(headR * 0.45f, headR * 0.26f))
            drawOval(fur, Offset(cx + headR * 0.79f, headCy - headR * 0.35f), Size(headR * 0.45f, headR * 0.26f))
            // Curved bull horns
            val leftHorn = Path().apply {
                moveTo(cx - headR * 0.56f, headCy - headR * 0.72f)
                quadraticTo(cx - headR * 1.15f, headCy - headR * 0.95f, cx - headR * 0.95f, headCy - headR * 1.38f)
                quadraticTo(cx - headR * 0.80f, headCy - headR * 0.96f, cx - headR * 0.36f, headCy - headR * 0.86f)
                close()
            }
            val rightHorn = Path().apply {
                moveTo(cx + headR * 0.56f, headCy - headR * 0.72f)
                quadraticTo(cx + headR * 1.15f, headCy - headR * 0.95f, cx + headR * 0.95f, headCy - headR * 1.38f)
                quadraticTo(cx + headR * 0.80f, headCy - headR * 0.96f, cx + headR * 0.36f, headCy - headR * 0.86f)
                close()
            }
            drawPath(leftHorn, color = hornColor)
            drawPath(rightHorn, color = hornColor)
        }
        AnimalSpecies.LION, AnimalSpecies.BEAR, AnimalSpecies.TIGER -> {
            val earOffset = headR * 0.72f
            val earY = headCy - headR * 0.76f
            if (appearance.animalSpecies == AnimalSpecies.TIGER) {
                drawCircle(Color(0xFF1F2937), radius = headR * 0.33f, center = Offset(cx - earOffset, earY))
                drawCircle(Color(0xFF1F2937), radius = headR * 0.33f, center = Offset(cx + earOffset, earY))
            }
            drawCircle(fur, radius = headR * 0.28f, center = Offset(cx - earOffset, earY))
            drawCircle(fur, radius = headR * 0.28f, center = Offset(cx + earOffset, earY))
            drawCircle(innerPink, radius = headR * 0.15f, center = Offset(cx - earOffset, earY))
            drawCircle(innerPink, radius = headR * 0.15f, center = Offset(cx + earOffset, earY))
        }
        AnimalSpecies.WOLF, AnimalSpecies.PANTHER, AnimalSpecies.FOX -> {
            val earH = if (appearance.animalSpecies == AnimalSpecies.PANTHER) 1.24f else 1.42f
            val leftEar = Path().apply {
                moveTo(cx - headR * 0.86f, headCy - headR * 0.42f)
                lineTo(cx - headR * 0.74f, headCy - headR * earH)
                lineTo(cx - headR * 0.24f, headCy - headR * 0.85f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(cx + headR * 0.86f, headCy - headR * 0.42f)
                lineTo(cx + headR * 0.74f, headCy - headR * earH)
                lineTo(cx + headR * 0.24f, headCy - headR * 0.85f)
                close()
            }
            drawPath(leftEar, color = fur)
            drawPath(rightEar, color = fur)
            val innerColor = if (appearance.animalSpecies == AnimalSpecies.FOX) Color(0xFFFDF8F2) else innerPink
            drawCircle(innerColor, radius = headR * 0.14f, center = Offset(cx - headR * 0.62f, headCy - headR * 0.88f))
            drawCircle(innerColor, radius = headR * 0.14f, center = Offset(cx + headR * 0.62f, headCy - headR * 0.88f))
        }
        AnimalSpecies.GORILLA -> {
            drawCircle(fur, radius = headR * 0.24f, center = Offset(cx - headR * 0.98f, headCy))
            drawCircle(fur, radius = headR * 0.24f, center = Offset(cx + headR * 0.98f, headCy))
        }
        AnimalSpecies.EAGLE -> {
            // Feathered head crest
            val crest = Path().apply {
                moveTo(cx - headR * 0.35f, headCy - headR * 0.86f)
                lineTo(cx - headR * 0.18f, headCy - headR * 1.28f)
                lineTo(cx, headCy - headR * 1.05f)
                lineTo(cx + headR * 0.18f, headCy - headR * 1.32f)
                lineTo(cx + headR * 0.38f, headCy - headR * 0.84f)
                close()
            }
            drawPath(crest, color = fur)
        }
    }
}

private fun DrawScope.drawAnimalMuzzleAndMouth(
    cx: Float,
    headCy: Float,
    headR: Float,
    appearance: AthleteAppearance,
    moodState: AvatarMoodState,
    lightPatch: Color,
) {
    if (appearance.animalSpecies == AnimalSpecies.EAGLE) {
        // Golden curved eagle beak
        val beak = Path().apply {
            moveTo(cx - headR * 0.26f, headCy + headR * 0.08f)
            lineTo(cx + headR * 0.26f, headCy + headR * 0.08f)
            quadraticTo(cx + headR * 0.16f, headCy + headR * 0.56f, cx, headCy + headR * 0.62f)
            quadraticTo(cx - headR * 0.16f, headCy + headR * 0.56f, cx - headR * 0.26f, headCy + headR * 0.08f)
            close()
        }
        drawPath(beak, color = Color(0xFFF59E0B))
        return
    }

    // Muzzle / snout patch
    val muzzleColor = when (appearance.animalSpecies) {
        AnimalSpecies.BULL -> Color(0xFFE7C4A8)
        AnimalSpecies.GORILLA -> Color(0xFFBC9676)
        AnimalSpecies.PANTHER -> appearance.skinColor.copy(alpha = 0.85f)
        else -> lightPatch
    }
    val muzzleW = if (appearance.animalSpecies == AnimalSpecies.BULL) headR * 1.15f else headR * 0.84f
    drawOval(
        color = muzzleColor,
        topLeft = Offset(cx - muzzleW * 0.5f, headCy + headR * 0.05f),
        size = Size(muzzleW, headR * 0.62f),
    )

    // Facial hair (if chosen)
    drawFacialHair(cx, headCy, headR, appearance)

    // Nose
    val noseY = headCy + headR * 0.18f
    when (appearance.animalSpecies) {
        AnimalSpecies.BULL -> {
            drawCircle(Color(0xFF334155), radius = headR * 0.06f, center = Offset(cx - headR * 0.18f, noseY))
            drawCircle(Color(0xFF334155), radius = headR * 0.06f, center = Offset(cx + headR * 0.18f, noseY))
            // Sporty golden bull ring
            drawCircle(
                color = Color(0xFFFACC15),
                radius = headR * 0.11f,
                center = Offset(cx, noseY + headR * 0.11f),
                style = Stroke(width = headR * 0.045f),
            )
        }
        AnimalSpecies.LION, AnimalSpecies.PANTHER, AnimalSpecies.FOX, AnimalSpecies.TIGER -> {
            val nosePath = Path().apply {
                moveTo(cx - headR * 0.14f, noseY - headR * 0.04f)
                lineTo(cx + headR * 0.14f, noseY - headR * 0.04f)
                lineTo(cx, noseY + headR * 0.10f)
                close()
            }
            val noseColor = if (appearance.animalSpecies == AnimalSpecies.TIGER) Color(0xFFE11D48) else Color(0xFF1F2937)
            drawPath(nosePath, color = noseColor)
        }
        else -> {
            drawOval(
                color = Color(0xFF1F2937),
                topLeft = Offset(cx - headR * 0.15f, noseY - headR * 0.07f),
                size = Size(headR * 0.30f, headR * 0.18f),
            )
        }
    }

    // Mouth expression
    val mouthY = headCy + headR * 0.42f
    val mouthPath = Path()
    when (moodState) {
        AvatarMoodState.CELEBRANDO_PR, AvatarMoodState.CONTENTO_MEJORA, AvatarMoodState.MOTIVADO -> {
            mouthPath.moveTo(cx - headR * 0.24f, mouthY - headR * 0.04f)
            mouthPath.quadraticTo(cx, mouthY + headR * 0.22f, cx + headR * 0.24f, mouthY - headR * 0.04f)
            drawPath(mouthPath, color = Color(0xFF1F2937), style = Stroke(width = headR * 0.07f, cap = StrokeCap.Round))
        }
        AvatarMoodState.DURMIENDO -> {
            drawCircle(Color(0xFF1F2937), radius = headR * 0.08f, center = Offset(cx, mouthY + headR * 0.04f))
        }
        AvatarMoodState.CANSADO -> {
            drawLine(
                color = Color(0xFF1F2937),
                start = Offset(cx - headR * 0.16f, mouthY + headR * 0.04f),
                end = Offset(cx + headR * 0.16f, mouthY + headR * 0.04f),
                strokeWidth = headR * 0.065f,
                cap = StrokeCap.Round,
            )
        }
        else -> {
            mouthPath.moveTo(cx - headR * 0.20f, mouthY)
            mouthPath.quadraticTo(cx, mouthY + headR * 0.14f, cx + headR * 0.20f, mouthY)
            drawPath(mouthPath, color = Color(0xFF1F2937), style = Stroke(width = headR * 0.068f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawFacialHair(
    cx: Float,
    headCy: Float,
    headR: Float,
    appearance: AthleteAppearance,
) {
    val beardColor = appearance.hairColor
    when (appearance.facialHair) {
        FacialHairStyle.NONE -> Unit
        FacialHairStyle.STUBBLE -> {
            drawArc(
                color = beardColor.copy(alpha = 0.28f),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - headR * 0.88f, headCy - headR * 0.35f),
                size = Size(headR * 1.76f, headR * 1.32f),
                style = Stroke(width = headR * 0.16f, cap = StrokeCap.Round),
            )
        }
        FacialHairStyle.MUSTACHE -> {
            val path = Path().apply {
                moveTo(cx - headR * 0.30f, headCy + headR * 0.28f)
                quadraticTo(cx - headR * 0.12f, headCy + headR * 0.16f, cx, headCy + headR * 0.25f)
                quadraticTo(cx + headR * 0.12f, headCy + headR * 0.16f, cx + headR * 0.30f, headCy + headR * 0.28f)
            }
            drawPath(path, color = beardColor, style = Stroke(width = headR * 0.11f, cap = StrokeCap.Round))
        }
        FacialHairStyle.SHORT_BEARD -> {
            drawArc(
                color = beardColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - headR * 0.90f, headCy - headR * 0.25f),
                size = Size(headR * 1.80f, headR * 1.24f),
                style = Stroke(width = headR * 0.22f, cap = StrokeCap.Round),
            )
        }
        FacialHairStyle.FULL_BEARD -> {
            drawArc(
                color = beardColor,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = true,
                topLeft = Offset(cx - headR * 0.96f, headCy - headR * 0.08f),
                size = Size(headR * 1.92f, headR * 1.32f),
            )
        }
    }
}

private fun DrawScope.drawHairTop(
    cx: Float,
    headCy: Float,
    headR: Float,
    appearance: AthleteAppearance,
) {
    if (appearance.animalSpecies == AnimalSpecies.EAGLE) return
    val hairColor = appearance.hairColor
    when (appearance.hairStyle) {
        HairStyle.BALD -> Unit
        HairStyle.BUZZ -> {
            drawArc(
                color = hairColor,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(cx - headR * 0.96f, headCy - headR * 0.98f),
                size = Size(headR * 1.92f, headR * 1.80f),
                style = Stroke(width = headR * 0.16f, cap = StrokeCap.Round),
            )
        }
        HairStyle.SHORT_SPIKY -> {
            val spikePath = Path().apply {
                moveTo(cx - headR * 0.56f, headCy - headR * 0.72f)
                lineTo(cx - headR * 0.42f, headCy - headR * 1.15f)
                lineTo(cx - headR * 0.14f, headCy - headR * 0.95f)
                lineTo(cx + headR * 0.05f, headCy - headR * 1.24f)
                lineTo(cx + headR * 0.28f, headCy - headR * 0.95f)
                lineTo(cx + headR * 0.54f, headCy - headR * 1.12f)
                lineTo(cx + headR * 0.62f, headCy - headR * 0.72f)
                close()
            }
            drawPath(spikePath, color = hairColor)
        }
        HairStyle.CURLY -> {
            val puffs = listOf(-0.46f, -0.22f, 0f, 0.22f, 0.46f)
            puffs.forEachIndexed { idx, xOff ->
                val yOff = -0.84f - (if (idx % 2 == 0) 0.10f else 0.20f)
                drawCircle(
                    color = hairColor,
                    radius = headR * 0.26f,
                    center = Offset(cx + headR * xOff, headCy + headR * yOff),
                )
            }
        }
        HairStyle.SIDE_PART, HairStyle.PONYTAIL, HairStyle.LONG_FLOW -> {
            drawArc(
                color = hairColor,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = true,
                topLeft = Offset(cx - headR * 0.92f, headCy - headR * 1.06f),
                size = Size(headR * 1.84f, headR * 0.90f),
            )
        }
    }
}

private fun DrawScope.drawCelebrationSparkles(
    cx: Float,
    headCy: Float,
    headR: Float,
    color: Color,
    phase: Float,
) {
    val angles = listOf(0.2f, 1.1f, 2.3f, 3.8f, 5.1f)
    angles.forEachIndexed { idx, baseAngle ->
        val angle = baseAngle + phase * 2f * PI.toFloat() * 0.5f
        val dist = headR * (1.45f + 0.25f * sin((phase + idx * 0.2f) * 2f * PI.toFloat()))
        val pos = Offset(cx + cos(angle) * dist, headCy + sin(angle) * dist * 0.7f)
        drawCircle(
            color = if (idx % 2 == 0) Color(0xFFFACC15) else color,
            radius = headR * 0.11f,
            center = pos,
        )
    }
}

private fun DrawScope.drawSleepBubbles(
    startX: Float,
    startY: Float,
    headR: Float,
    phase: Float,
) {
    for (i in 0..2) {
        val drift = ((phase + i * 0.33f) % 1f)
        val x = startX + drift * headR * 0.6f
        val y = startY - drift * headR * 1.1f
        val r = headR * (0.10f + i * 0.04f)
        drawCircle(
            color = Color(0xFF94A3B8).copy(alpha = (1f - drift).coerceIn(0.15f, 0.75f)),
            radius = r,
            center = Offset(x, y),
            style = Stroke(width = 3f, join = StrokeJoin.Round),
        )
    }
}
