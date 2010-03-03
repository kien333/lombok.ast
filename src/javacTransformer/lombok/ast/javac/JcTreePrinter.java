/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.ast.javac;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.util.List;

public class JcTreePrinter extends JCTree.Visitor {
	private final StringBuilder output = new StringBuilder();
	private int indent;
	private String rel;
	
	private static final Method GET_TAG_METHOD;
	private static final Field TAG_FIELD;
	
	static {
		Method m = null;
		Field f = null;
		try {
			m = JCTree.class.getDeclaredMethod("getTag");
		} catch (NoSuchMethodException e) {
			try {
				f = JCTree.class.getDeclaredField("tag");
			} catch (NoSuchFieldException e1) {
				e1.printStackTrace();
			}
		}
		GET_TAG_METHOD = m;
		TAG_FIELD = f;
	}
	
	static int getTag(JCTree tree) {
		if (GET_TAG_METHOD != null) {
			try {
				return (Integer) GET_TAG_METHOD.invoke(tree);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			}
		}
		try {
			return TAG_FIELD.getInt(tree);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String toString() {
		return output.toString();
	}
	
	private void printNode(JCTree nodeKind) {
		printNode(nodeKind == null ? "NULL" : nodeKind.getClass().getSimpleName());
	}
	
	private void printNode(String nodeKind) {
		printIndent();
		if (rel != null)
			output.append(rel).append(": ");
		rel = null;
		output.append("[").append(nodeKind).append("]\n");
		indent++;
	}
	
	private void printIndent() {
		for (int i = 0; i < indent; i++) {
			output.append("\t");
		}
	}
	
	private void property(String rel, Object val) {
		printIndent();
		if (rel != null)
			output.append(rel).append(": ");
		if (val instanceof JCTree)
			output.append("!!JCT-AS-PROP!!");
		if (val == null) {
			output.append("[NULL]\n");
		} else {
			output.append("[").append(val.getClass().getSimpleName()).append(" ").append(val.toString()).append("]\n");
		}
	}
	
	private void child(String rel, JCTree node) {
		this.rel = rel;
		if (node != null)
			node.accept(this);
		else {
			printNode("NULL");
			indent--;
		}
	}
	
	private void children(String rel, List<? extends JCTree> nodes) {
		this.rel = rel;
		
		if (nodes == null) {
			;
			printNode("LISTNULL");
			indent--;
		} else if (nodes.isEmpty()) {
			printNode("LISTEMPTY");
			indent--;
		} else {
			int i = 0;
			for (JCTree node : nodes) {
				child(String.format("%s[%d]", rel, i++), node);
			}
		}
	}
	
	public void visitTopLevel(JCCompilationUnit tree) {
		printNode(tree);
		child("pid", tree.pid);
		children("defs", tree.defs);
		indent--;
	}
	
	public void visitImport(JCImport tree) {
		printNode(tree);
		property("staticImport", tree.staticImport);
		child("qualid", tree.qualid);
		indent--;
	}
	
	public void visitClassDef(JCClassDecl tree) {
		printNode(tree);
		child("mods", tree.mods);
		property("name", tree.name);
		children("typarams", tree.typarams);
		child("extends", tree.extending);
		children("implementing", tree.implementing);
		children("defs", tree.defs);
		indent--;
	}
	
	public void visitMethodDef(JCMethodDecl tree) {
		printNode(tree);
		property("name", tree.name);
		child("mods", tree.mods);
		children("typarams", tree.typarams);
		children("params", tree.params);
		children("thrown", tree.thrown);
		child("body", tree.body);
		indent--;
	}
	
	public void visitVarDef(JCVariableDecl tree) {
		printNode(tree);
		child("mods", tree.mods);
		child("vartype", tree.vartype);
		property("name", tree.name);
		child("init", tree.init);
		indent--;
	}
	
	public void visitSkip(JCSkip tree) {
		printNode(tree);
		indent--;
	}
	
	public void visitBlock(JCBlock tree) {
		printNode(tree);
		property("flags", "0x" + Long.toString(tree.flags, 0x10));
		children("stats", tree.stats);
		indent--;
	}
	
	public void visitDoLoop(JCDoWhileLoop tree) {
		printNode(tree);
		child("body", tree.body);
		child("cond", tree.cond);
		indent--;
	}
	
	public void visitWhileLoop(JCWhileLoop tree) {
		printNode(tree);
		child("cond", tree.cond);
		child("body", tree.body);
		indent--;
	}
	
	public void visitForLoop(JCForLoop tree) {
		printNode(tree);
		children("init", tree.init);
		child("cond", tree.cond);
		children("step", tree.step);
		child("body", tree.body);
		indent--;
	}
	
	public void visitForeachLoop(JCEnhancedForLoop tree) {
		printNode(tree);
		child("var", tree.var);
		child("expr", tree.expr);
		child("body", tree.body);
		indent--;
	}
	
	public void visitLabelled(JCLabeledStatement tree) {
		printNode(tree);
		property("label", tree.label);
		child("body", tree.body);
		indent--;
	}
	
	public void visitSwitch(JCSwitch tree) {
		printNode(tree);
		child("selector", tree.selector);
		children("cases", tree.cases);
		indent--;
	}
	
	public void visitCase(JCCase tree) {
		printNode(tree);
		child("pat", tree.pat);
		children("stats", tree.stats);
		indent--;
	}
	
	public void visitSynchronized(JCSynchronized tree) {
		printNode(tree);
		child("lock", tree.lock);
		child("body", tree.body);
		indent--;
	}
	
	public void visitTry(JCTry tree) {
		printNode(tree);
		child("body", tree.body);
		children("catchers", tree.catchers);
		child("finalizer", tree.finalizer);
		indent--;
	}
	
	public void visitCatch(JCCatch tree) {
		printNode(tree);
		child("param", tree.param);
		child("body", tree.body);
		indent--;
	}
	
	public void visitConditional(JCConditional tree) {
		printNode(tree);
		child("cond", tree.cond);
		child("truepart", tree.truepart);
		child("falsepart", tree.falsepart);
		indent--;
	}
	
	public void visitIf(JCIf tree) {
		printNode(tree);
		child("cond", tree.cond);
		child("thenpart", tree.thenpart);
		child("elsepart", tree.elsepart);
		indent--;
	}
	
	public void visitExec(JCExpressionStatement tree) {
		printNode(tree);
		child("expr", tree.expr);
		indent--;
	}
	
	public void visitBreak(JCBreak tree) {
		printNode(tree);
		property("label", tree.label);
		indent--;
	}
	
	public void visitContinue(JCContinue tree) {
		printNode(tree);
		property("label", tree.label);
		indent--;
	}
	
	public void visitReturn(JCReturn tree) {
		printNode(tree);
		child("expr", tree.expr);
		indent--;
	}
	
	public void visitThrow(JCThrow tree) {
		printNode(tree);
		child("expr", tree.expr);
		indent--;
	}
	
	public void visitAssert(JCAssert tree) {
		printNode(tree);
		child("cond", tree.cond);
		child("detail", tree.detail);
		indent--;
	}
	
	public void visitApply(JCMethodInvocation tree) {
		printNode(tree);
		children("typeargs", tree.typeargs);
		child("meth", tree.meth);
		children("args", tree.args);
		indent--;
	}
	
	public void visitNewClass(JCNewClass tree) {
		printNode(tree);
		child("encl", tree.encl);
		children("typeargs", tree.typeargs);
		child("clazz", tree.clazz);
		children("args", tree.args);
		child("def", tree.def);
		indent--;
	}
	
	public void visitNewArray(JCNewArray tree) {
		printNode(tree);
		child("elemtype", tree.elemtype);
		children("dims", tree.dims);
		children("elems", tree.elems);
		indent--;
	}
	
	public void visitParens(JCParens tree) {
		printNode(tree);
		child("expr", tree.expr);
		indent--;
	}
	
	public void visitAssign(JCAssign tree) {
		printNode(tree);
		child("lhs", tree.lhs);
		child("rhs", tree.rhs);
		indent--;
	}
	
	public String operatorName(int tag) {
		switch (tag) {
		case JCTree.POS:
			return "+";
		case JCTree.NEG:
			return "-";
		case JCTree.NOT:
			return "!";
		case JCTree.COMPL:
			return "~";
		case JCTree.PREINC:
			return "++X";
		case JCTree.PREDEC:
			return "--X";
		case JCTree.POSTINC:
			return "X++";
		case JCTree.POSTDEC:
			return "X--";
		case JCTree.NULLCHK:
			return "<*nullchk*>";
		case JCTree.OR:
			return "||";
		case JCTree.AND:
			return "&&";
		case JCTree.EQ:
			return "==";
		case JCTree.NE:
			return "!=";
		case JCTree.LT:
			return "<";
		case JCTree.GT:
			return ">";
		case JCTree.LE:
			return "<=";
		case JCTree.GE:
			return ">=";
		case JCTree.BITOR:
			return "|";
		case JCTree.BITXOR:
			return "^";
		case JCTree.BITAND:
			return "&";
		case JCTree.SL:
			return "<<";
		case JCTree.SR:
			return ">>";
		case JCTree.USR:
			return ">>>";
		case JCTree.PLUS:
			return "+";
		case JCTree.MINUS:
			return "-";
		case JCTree.MUL:
			return "*";
		case JCTree.DIV:
			return "/";
		case JCTree.MOD:
			return "%";
		default:
			throw new Error();
		}
	}
	
	public void visitAssignop(JCAssignOp tree) {
		printNode(tree);
		child("lhs", tree.lhs);
		property("(operator)", operatorName(getTag(tree) - JCTree.ASGOffset) + "=");
		child("rhs", tree.rhs);
		indent--;
	}
	
	public void visitUnary(JCUnary tree) {
		printNode(tree);
		child("arg", tree.arg);
		property("(operator)", operatorName(getTag(tree)));
		indent--;
	}
	
	public void visitBinary(JCBinary tree) {
		printNode(tree);
		child("lhs", tree.lhs);
		property("(operator)", operatorName(getTag(tree)));
		child("rhs", tree.rhs);
		indent--;
	}
	
	public void visitTypeCast(JCTypeCast tree) {
		printNode(tree);
		child("clazz", tree.clazz);
		child("expr", tree.expr);
		indent--;
	}
	
	public void visitTypeTest(JCInstanceOf tree) {
		printNode(tree);
		child("expr", tree.expr);
		child("clazz", tree.clazz);
		indent--;
	}
	
	public void visitIndexed(JCArrayAccess tree) {
		printNode(tree);
		child("indexed", tree.indexed);
		child("index", tree.index);
		indent--;
	}
	
	public void visitSelect(JCFieldAccess tree) {
		printNode(tree);
		child("selected", tree.selected);
		property("name", tree.name);
		indent--;
	}
	
	public void visitIdent(JCIdent tree) {
		printNode(tree);
		property("name", tree.name);
		indent--;
	}
	
	public String literalName(int typeTag, boolean stringOkay) {
		switch (typeTag) {
		case TypeTags.BYTE:
			return "BYTE";
		case TypeTags.SHORT:
			return "SHORT";
		case TypeTags.INT:
			return "INT";
		case TypeTags.LONG:
			return "LONG";
		case TypeTags.FLOAT:
			return "FLOAT";
		case TypeTags.DOUBLE:
			return "DOUBLE";
		case TypeTags.CHAR:
			return "CHAR";
		case TypeTags.BOOLEAN:
			return "BOOLEAN";
		case TypeTags.VOID:
			return "VOID";
		case TypeTags.BOT:
			return "BOT";
		default:
			return stringOkay ? ("STRING(" + typeTag + ")") : ("ERROR(" + typeTag + ")");
		}
	}
	
	public void visitLiteral(JCLiteral tree) {
		printNode(tree);
		property("typetag", literalName(tree.typetag, true));
		property("value", tree.value);
		indent--;
	}
	
	public void visitTypeIdent(JCPrimitiveTypeTree tree) {
		printNode(tree);
		property("typetag", literalName(tree.typetag, false));
		indent--;
	}
	
	public void visitTypeArray(JCArrayTypeTree tree) {
		printNode(tree);
		child("elemtype", tree.elemtype);
		indent--;
	}
	
	public void visitTypeApply(JCTypeApply tree) {
		printNode(tree);
		child("clazz", tree.clazz);
		children("arguments", tree.arguments);
		indent--;
	}
	
	public void visitTypeParameter(JCTypeParameter tree) {
		printNode(tree);
		property("name", tree.name);
		children("bounds", tree.bounds);
		indent--;
	}
	
	public void visitWildcard(JCWildcard tree) {
		printNode(tree);
		Object o = tree.kind;
		if (o instanceof JCTree) {
			child("kind", (JCTree)o);
		} else if (o instanceof BoundKind) {
			property("kind", String.valueOf(o));
		}
		child("inner", tree.inner);
		indent--;
	}
	
	public void visitTypeBoundKind(TypeBoundKind tree) {
		printNode(tree);
		property("kind", String.valueOf(tree.kind));
		indent--;
	}
	
	public void visitErroneous(JCErroneous tree) {
		printNode(tree);
		children("errs", tree.errs);
		indent--;
	}
	
	public void visitLetExpr(LetExpr tree) {
		printNode(tree);
		children("defs", tree.defs);
		child("expr", tree.expr);
		indent--;
	}
	
	public void visitModifiers(JCModifiers tree) {
		printNode(tree);
		children("annotations", tree.annotations);
		property("flags", "0x" + Long.toString(tree.flags, 0x10));
		indent--;
	}
	
	public void visitAnnotation(JCAnnotation tree) {
		printNode(tree);
		child("annotationType", tree.annotationType);
		children("args", tree.args);
		indent--;
	}
	
	public void visitTree(JCTree tree) {
		String typeName = tree == null ? "NULL" : tree.getClass().getSimpleName();
		printNode("UNKNOWN(" + typeName + ")");
		indent--;
	}
}