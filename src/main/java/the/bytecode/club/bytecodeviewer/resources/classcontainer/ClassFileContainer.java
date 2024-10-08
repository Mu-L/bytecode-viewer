package the.bytecode.club.bytecodeviewer.resources.classcontainer;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import the.bytecode.club.bytecodeviewer.resources.ResourceContainer;
import the.bytecode.club.bytecodeviewer.resources.classcontainer.locations.*;
import the.bytecode.club.bytecodeviewer.resources.classcontainer.parser.MyVoidVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a container for a specific class. The container name is based on the actual class name and the decompiler used.
 * <p>
 * Created by Bl3nd.
 * Date: 8/26/2024
 */
public class ClassFileContainer
{
    public transient NavigableMap<String, ArrayList<ClassFieldLocation>> fieldMembers = new TreeMap<>();
    public transient NavigableMap<String, ArrayList<ClassParameterLocation>> methodParameterMembers = new TreeMap<>();
    public transient NavigableMap<String, ArrayList<ClassLocalVariableLocation>> methodLocalMembers = new TreeMap<>();
    public transient NavigableMap<String, ArrayList<ClassMethodLocation>> methodMembers = new TreeMap<>();
    public transient NavigableMap<String, ArrayList<ClassReferenceLocation>> classReferences = new TreeMap<>();

    public boolean hasBeenParsed = false;
    public final String className;
    private final String content;
    private final String parentContainer;
    private final String path;

    public ClassFileContainer(String className, String content, ResourceContainer resourceContainer)
    {
        this.className = className;
        this.content = content;
        this.parentContainer = resourceContainer.name;
        this.path = resourceContainer.file.getAbsolutePath();
    }

    /**
     * Parse the class content with JavaParser.
     */
    public void parse()
    {
        try
        {
            TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(false), new JarTypeSolver(path));
            StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
            CompilationUnit compilationUnit = StaticJavaParser.parse(this.content);
            compilationUnit.accept(new MyVoidVisitor(this, compilationUnit), null);
        }
        catch (java.lang.ClassCastException | UnsolvedSymbolException | ParseProblemException e)
        {
            System.err.println("Parsing error!");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getName()
    {
        return this.className.substring(this.className.lastIndexOf('/') + 1, this.className.lastIndexOf('.'));
    }

    public String getDecompiler()
    {
        return this.className.substring(this.className.lastIndexOf('-') + 1);
    }

    public String getParentContainer()
    {
        return this.parentContainer;
    }

    public void putField(String key, ClassFieldLocation value)
    {
        this.fieldMembers.computeIfAbsent(key, v -> new ArrayList<>()).add(value);
    }

    public List<ClassFieldLocation> getFieldLocationsFor(String fieldName)
    {
        return fieldMembers.getOrDefault(fieldName, new ArrayList<>());
    }

    public void putParameter(String key, ClassParameterLocation value)
    {
        this.methodParameterMembers.computeIfAbsent(key, v -> new ArrayList<>()).add(value);
    }

    public List<ClassParameterLocation> getParameterLocationsFor(String key)
    {
        return methodParameterMembers.getOrDefault(key, new ArrayList<>());
    }

    public void putLocalVariable(String key, ClassLocalVariableLocation value)
    {
        this.methodLocalMembers.computeIfAbsent(key, v -> new ArrayList<>()).add(value);
    }

    public List<ClassLocalVariableLocation> getLocalLocationsFor(String key)
    {
        return methodLocalMembers.getOrDefault(key, new ArrayList<>());
    }

    public void putMethod(String key, ClassMethodLocation value)
    {
        this.methodMembers.computeIfAbsent(key, v -> new ArrayList<>()).add(value);
    }

    public List<ClassMethodLocation> getMethodLocationsFor(String key)
    {
        return methodMembers.getOrDefault(key, new ArrayList<>());
    }

    public void putClassReference(String key, ClassReferenceLocation value)
    {
        this.classReferences.computeIfAbsent(key, v -> new ArrayList<>()).add(value);
    }

    public List<ClassReferenceLocation> getClassReferenceLocationsFor(String key)
    {
        return classReferences.getOrDefault(key, null);
    }

    public String getClassForField(String fieldName)
    {
        AtomicReference<String> className = new AtomicReference<>("");
        this.classReferences.forEach((s, v) ->
        {
            v.forEach(classReferenceLocation ->
            {
                if (classReferenceLocation.fieldName.equals(fieldName))
                {
                    className.set(classReferenceLocation.packagePath + "/" + s);
                }
            });
        });

        return className.get();
    }
}
