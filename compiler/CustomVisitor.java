package compiler;

import java.util.HashMap;

import org.objectweb.asm.*;

/**
 * This class overrides the methods in the antlr-generated BaseVisitor class to traverse a parse tree
 * 
 * @author Kirin Sharma
 * @version 1.0
 * Assignment 5
 * CS322 - Compiler Construction
 * Spring 2024
 */


public class CustomVisitor<T> extends lexparse.KnightCodeBaseVisitor<T>
{
    
    public HashMap<String, Variable> symbolTable; // the symbol table for all variables
    public int indexCount; // keeps track of current memory location for variable assignment
    public String outputName; // the name of the output file
    public ClassWriter cw; // the ASM class writer used in various methods to write bytecode
    public MethodVisitor mv; // the ASM method visitor used in various methods to write bytecode


    /**
     * Preferred constructor initializes filename
     */
    public CustomVisitor(String fileName)
    {
        symbolTable = new HashMap<String, Variable>(); 
        indexCount = 0;
        outputName = fileName;
    } // end preferred constructor

    
    @Override
    /**
     * Contains ASM code to create the class and main method of the .class file
     */
    public T visitFile(lexparse.KnightCodeParser.FileContext ctx) 
    {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, outputName, null, "java/lang/Object",null);
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        return visitChildren(ctx); 
    } // end visitFile


    @Override
    /**
     * Contains ASM code to declare a variable
     */
    public T visitVariable(lexparse.KnightCodeParser.VariableContext ctx) 
    {
        String dataType = ctx.getChild(0).getText(); // gets the left grandchild of the variable subtree, which is the data type
        String identifier = ctx.getChild(1).getText(); // gets the right grandchild of the variable subtree, which is the name of the identifier
        
        if(dataType.equals("INTEGER"))
        {
            symbolTable.put(identifier, new Variable("Integer", indexCount)); // declares a new Integer in the symbol table with a free memory location
            indexCount++; 
        }
        else if(dataType.equals("STRING"))
        {
            symbolTable.put(identifier, new Variable("String", indexCount)); // declares a new String in the symbol table with a free memory location
            indexCount++; 
        }

        // System.out.println("Created a variable.");
        
        return visitChildren(ctx); 
    } // end visitVariable


    @Override
    /**
     * Contains ASM code to assign a value to a variable
     */
    public T visitSetvar(lexparse.KnightCodeParser.SetvarContext ctx)
    {
        String identifier = ctx.getChild(1).getText();

        if(symbolTable.get(identifier).getDataType().equals("Integer"))
        {
            String val = ctx.getChild(3).getText();

            // If val is not a single integer, visit children and perform necessary operations
            if(ctx.getChild(3).getChildCount() != 1)
            {
                visit(ctx.getChild(3));
                mv.visitVarInsn(Opcodes.ISTORE, symbolTable.get(identifier).getMemoryLocation());
            }
            else 
            {
                // Load integer into memory
                Integer value = Integer.parseInt(val);
                mv.visitLdcInsn(value);
                mv.visitVarInsn(Opcodes.ISTORE, symbolTable.get(identifier).getMemoryLocation());
            }
        }
        else
        {
            // Load String into memory 
            String value = ctx.getChild(3).getText();
            mv.visitLdcInsn(value);
            mv.visitVarInsn(Opcodes.ASTORE, symbolTable.get(identifier).getMemoryLocation());
        }

        return visitChildren(ctx);
    } // end visitSetVar


    @Override
    /**
     * Contains ASM code to print either a variable or a string literal
     */
    public T visitPrint(lexparse.KnightCodeParser.PrintContext ctx)
    {
        String identifier = ctx.getChild(1).getText();

        // Check if what is being printed is a string literal
        if(!symbolTable.containsKey(identifier))
        {
            // Print a String literal
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(identifier);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            return visitChildren(ctx);
        }

        // Prints a variable
        if(symbolTable.get(identifier).getDataType().equals("Integer"))
        {
            // Print an integer variable
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(identifier).getMemoryLocation());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
        }
        else
        {
            // Print a String variable
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(Opcodes.ALOAD, symbolTable.get(identifier).getMemoryLocation());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
        
        return visitChildren(ctx);
    } // end visitPrint


    /**
     * Contains ASM code to write the footer of a class file
     */
    public void writeFooter()
    {
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
    } // end writeFooter


    /**
     * Returns the .class file as an array of bytes
     * @return
     */
    public byte[] getByteArray()
    {
        return cw.toByteArray();
    } // end getByteArray


    @Override
    /** 
     * Contains ASM code to add numbers, including logic to handle multiple additions
     */ 
    public T visitAddition(lexparse.KnightCodeParser.AdditionContext ctx) 
    { 
        // Load the first operand onto the stack (left subtree traversal)
        if(ctx.getChild(0).getChildCount() == 1)
        {
            String operand1 = ctx.getChild(0).getText();
            // Check if first operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand1))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand1).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand1)); // load the leftmost integer onto the stack
            }
        }
        else // visits the left child recursively
        {
            visit(ctx.getChild(0));
        }

        // Load the second operand onto the stack (right subtree traversal)
        if(ctx.getChild(2).getChildCount() == 1)
        {
            String operand2 = ctx.getChild(2).getText();
            // Check if second operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand2))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand2).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand2));
            }
        }
        else // visits right child recursively
        {
            visit(ctx.getChild(2));
        }

        mv.visitInsn(Opcodes.IADD); // add two operands and have result on top of stack

        return null;

    } // end visitAddition


    @Override
    /**
     * Contains ASM code to multiply two integers, including logic to handle multiple multiplications
     */
    public T visitMultiplication(lexparse.KnightCodeParser.MultiplicationContext ctx) 
    {
        // Load the first operand onto the stack (left subtree traversal)
        if(ctx.getChild(0).getChildCount() == 1)
        {
            String operand1 = ctx.getChild(0).getText();
            // Check if first operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand1))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand1).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand1)); // load the leftmost integer onto the stack
            }
        }
        else // visits the left child recursively
        {
            visit(ctx.getChild(0));
        }

        // Load the second operand onto the stack (right subtree traversal)
        if(ctx.getChild(2).getChildCount() == 1)
        {
            String operand2 = ctx.getChild(2).getText();
            // Check if second operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand2))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand2).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand2));
            }
        }
        else // visits right child recursively
        {
            visit(ctx.getChild(2));
        }

        mv.visitInsn(Opcodes.IMUL); // multiply two operands and have result on top of stack

        return null;
    } // end visitMultiplication


    @Override
    /**
     * Contains ASM code to multiply two integers, including logic to handle multiple multiplications
     */
    public T visitSubtraction(lexparse.KnightCodeParser.SubtractionContext ctx)
    {
        // Load the first operand onto the stack (left subtree traversal)
        if(ctx.getChild(0).getChildCount() == 1)
        {
            String operand1 = ctx.getChild(0).getText();
            // Check if first operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand1))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand1).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand1)); // load the leftmost integer onto the stack
            }
        }
        else // visits the left child recursively
        {
            visit(ctx.getChild(0));
        }

        // Load the second operand onto the stack (right subtree traversal)
        if(ctx.getChild(2).getChildCount() == 1)
        {
            String operand2 = ctx.getChild(2).getText();
            // Check if second operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand2))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand2).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand2));
            }
        }
        else // visits right child recursively
        {
            visit(ctx.getChild(2));
        }

        mv.visitInsn(Opcodes.ISUB); // subtract two operands and have result on top of stack

        return null;
    } // end visitSubtraction

    @Override
    /**
     * Contains ASM code to multiply two integers, including logic to handle multiple multiplications
     */
    public T visitDivision(lexparse.KnightCodeParser.DivisionContext ctx)
    {
        // Load the first operand onto the stack (left subtree traversal)
        if(ctx.getChild(0).getChildCount() == 1)
        {
            String operand1 = ctx.getChild(0).getText();
            // Check if first operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand1))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand1).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand1)); // load the leftmost integer onto the stack
            }
        }
        else // visits the left child recursively
        {
            visit(ctx.getChild(0));
        }

        // Load the second operand onto the stack (right subtree traversal)
        if(ctx.getChild(2).getChildCount() == 1)
        {
            String operand2 = ctx.getChild(2).getText();
            // Check if second operand is a variable or literal integer and load onto stack
            if(symbolTable.containsKey(operand2))
            {
                mv.visitVarInsn(Opcodes.ILOAD, symbolTable.get(operand2).getMemoryLocation());
            }
            else
            {
                mv.visitLdcInsn(Integer.parseInt(operand2));
            }
        }
        else // visits right child recursively
        {
            visit(ctx.getChild(2));
        }

        mv.visitInsn(Opcodes.IDIV); // subtract two operands and have result on top of stack

        return null;
    } // end visitDivision


    @Override 
    /**
     * Contains logic to write bytecode to read in an integer or string to a variable
     */
    public T visitRead(lexparse.KnightCodeParser.ReadContext ctx) 
    {
        // Initialize and store scanner object if there is not already one
        if(!symbolTable.containsKey("Scanner"))
        {
            symbolTable.put("Scanner", new Variable("SCANNER", indexCount));
            mv.visitTypeInsn(Opcodes.NEW, "java/util/Scanner");
            mv.visitInsn(Opcodes.DUP);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;"); // load System.in onto stack
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
            mv.visitVarInsn(Opcodes.ASTORE, indexCount);
            indexCount++;
        }
        
        // Load scanner onto stack
        mv.visitVarInsn(Opcodes.ALOAD, symbolTable.get("Scanner").getMemoryLocation());

        // Get the next string or int depending on what the variable type we are reading into is
        Variable var = symbolTable.get(ctx.getChild(1).getText());
        if(var.getDataType().equals("String"))
        {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Scanner", "nextLine", "()Ljava/lang/String;", false);
            mv.visitVarInsn(Opcodes.ASTORE, var.getMemoryLocation());
        }
        else if(var.getDataType().equals("Integer"))
        {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false);
            mv.visitVarInsn(Opcodes.ISTORE, var.getMemoryLocation());
        }

        return visitChildren(ctx); 
    }


} // end class
