package com.creditdb.pro.data

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class EraStat(val year: Int, val mean: Double, val std: Double)

class TreeNode(
    val isLeaf: Boolean,
    val leafValue: Float = 0f,
    val splitFeature: Int = -1,
    val threshold: Float = 0f,
    val leftChild: TreeNode? = null,
    val rightChild: TreeNode? = null
) {
    fun evaluate(features: FloatArray): Float {
        if (isLeaf) return leafValue
        val featVal = if (splitFeature in features.indices) features[splitFeature] else 0f
        return if (featVal <= threshold) {
            leftChild?.evaluate(features) ?: 0f
        } else {
            rightChild?.evaluate(features) ?: 0f
        }
    }
}

class PredictionEngine private constructor(context: Context) {

    private val trees = mutableListOf<TreeNode>()
    private val featureNames = mutableListOf<String>()
    private val featureIndexMap = mutableMapOf<String, Int>()
    var globalMean: Double = 65.0
        private set
    private val roleM = mutableMapOf<String, Double>()

    init {
        loadModel(context)
    }

    private fun loadModel(context: Context) {
        try {
            context.assets.open("predictor_model.json").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val content = reader.readText()
                val rootJson = JSONObject(content)

                globalMean = rootJson.optDouble("global_mean", 65.0)

                val mObj = rootJson.optJSONObject("role_m")
                if (mObj != null) {
                    val keys = mObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        roleM[k] = mObj.getDouble(k)
                    }
                }

                val fArray = rootJson.getJSONArray("feature_names")
                for (i in 0 until fArray.length()) {
                    val name = fArray.getString(i)
                    featureNames.add(name)
                    featureIndexMap[name] = i
                }

                val treeInfoArray = rootJson.getJSONArray("tree_info")
                for (i in 0 until treeInfoArray.length()) {
                    val treeObj = treeInfoArray.getJSONObject(i)
                    val treeStructure = treeObj.getJSONObject("tree_structure")
                    trees.add(parseTreeNode(treeStructure))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseTreeNode(nodeJson: JSONObject): TreeNode {
        if (nodeJson.has("leaf_value")) {
            return TreeNode(
                isLeaf = true,
                leafValue = nodeJson.getDouble("leaf_value").toFloat()
            )
        }
        val splitFeat = nodeJson.getInt("split_feature")
        val thresh = nodeJson.getDouble("threshold").toFloat()
        val leftChild = if (nodeJson.has("left_child")) parseTreeNode(nodeJson.getJSONObject("left_child")) else null
        val rightChild = if (nodeJson.has("right_child")) parseTreeNode(nodeJson.getJSONObject("right_child")) else null

        return TreeNode(
            isLeaf = false,
            splitFeature = splitFeat,
            threshold = thresh,
            leftChild = leftChild,
            rightChild = rightChild
        )
    }

    fun getM(role: String): Double {
        return roleM[role] ?: roleM["all"] ?: 4.0
    }

    fun predict(
        featuresMap: Map<String, Float>,
        releaseYear: Int,
        eraStat: EraStat?
    ): PredictionResult {
        val fArray = FloatArray(featureNames.size)
        for (i in featureNames.indices) {
            val name = featureNames[i]
            fArray[i] = featuresMap[name] ?: 0f
        }

        var predZ = 0f
        for (tree in trees) {
            predZ += tree.evaluate(fArray)
        }

        val zVal = predZ.toDouble()
        val mean = eraStat?.mean ?: 0.0
        val std = if (eraStat != null && eraStat.std > 1e-6) eraStat.std else 1.0
        val estB = zVal * std + mean
        val estScore = (globalMean + estB).coerceIn(10.0, 100.0)
        val devScore = 50.0 + 10.0 * zVal
        val tier = calculateTier(devScore)

        val verdict = when {
            zVal > 0.4 -> "サプライズ名作候補 (高ポテンシャル)"
            zVal < -0.4 -> "慎重な見極めが必要 (低ポテンシャル)"
            else -> "概ね平均的なポテンシャル"
        }

        return PredictionResult(
            title = "新規企画アニメ",
            year = releaseYear,
            predictedZ = Math.round(zVal * 1000.0) / 1000.0,
            predictedScore = Math.round(estScore * 10.0) / 10.0,
            deviationScore = Math.round(devScore * 10.0) / 10.0,
            tier = tier,
            verdict = verdict
        )
    }

    private fun calculateTier(devScore: Double): String {
        return when {
            devScore >= 70.0 -> "S+"
            devScore >= 65.0 -> "S"
            devScore >= 60.0 -> "A+"
            devScore >= 55.0 -> "A"
            devScore >= 50.0 -> "B+"
            devScore >= 45.0 -> "B"
            devScore >= 40.0 -> "C"
            else -> "D"
        }
    }

    companion object {
        @Volatile
        private var instance: PredictionEngine? = null

        fun getInstance(context: Context): PredictionEngine {
            return instance ?: synchronized(this) {
                instance ?: PredictionEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
