package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NoConsecutiveBlankLinesRule : Rule("no-consecutive-blank-lines") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is PsiWhiteSpace &&
            node.prevSibling != null
        ) {
            val text = node.getText()
            val newLineCount = text.count { it == '\n' }
            if (newLineCount < 2) {
                return
            }

            val eof = node.nextLeaf() == null
            val betweenClassAndPrimaryConstructor = node.isBetweenClassAndPrimaryConstructor()

            if (newLineCount > 2 || eof || betweenClassAndPrimaryConstructor) {
                val split = text.split("\n")
                val offset =
                    node.startOffset +
                        split[0].length +
                        split[1].length +
                        if (betweenClassAndPrimaryConstructor) {
                            1
                        } else {
                            2
                        }
                emit(offset, "Needless blank line(s)", true)
                if (autoCorrect) {
                    val newText = buildString {
                        append(split.first())
                        append("\n")
                        if (!eof && !betweenClassAndPrimaryConstructor) append("\n")
                        append(split.last())
                    }
                    (node as LeafPsiElement).rawReplaceWithText(newText)
                }
            }
        }
    }

    private fun ASTNode.isBetweenClassAndPrimaryConstructor() =
        prevCodeLeaf()
            ?.let { prevNode ->
                prevNode.elementType == IDENTIFIER &&
                    prevNode.treeParent.elementType == CLASS &&
                    this.treeNext.elementType == PRIMARY_CONSTRUCTOR
            }
            ?: false
}
