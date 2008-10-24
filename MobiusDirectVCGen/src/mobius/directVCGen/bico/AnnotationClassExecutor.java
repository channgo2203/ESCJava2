package mobius.directVCGen.bico;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import mobius.bico.bicolano.coq.CoqStream;
import mobius.bico.executors.ClassExecutor;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;


/**
 * Write the normal class definitions like a normal executor,
 * plus a file for the annotations.
 * 
 * @author J. Charles (julien.charles@inria.fr)
 */
public class AnnotationClassExecutor extends ClassExecutor {
  /** the current class which is inspected. */
  private ClassGen fClass;
  

  
  /**
   * Create an executor that can produce annotations.
   * @param be the parent executor.
   * @param cg the class which is currently treated
   * @throws FileNotFoundException in case the file cannot be written on the disk
   */
  public AnnotationClassExecutor(final AnnotationExecutor be, 
                                 final ClassGen cg, 
                                 final IAnnotationGenerator gen) throws FileNotFoundException {
    super(be, cg);
    fClass = cg;
    gen.annotateClass(fClass);
  }
  
 
  
  /**
   * Do the annotation definition for each class.
   */
  @Override
  public void doClassDefinition() {
    super.doClassDefinition();
    

    try {
      final Method[] methods = fClass.getMethods(); 
      
      final CoqStream anOut = new CoqStream(new FileOutputStream(
                                         new File(getWorkingDir(), 
                                                  getModuleName() + "_annotations.v")));
    
      anOut.println(getLibPath());
      
      anOut.println("Require Export defs_types.");
      anOut.println("Require Export Bool.");
      anOut.println("Require Export Sumbool.");
      anOut.println("Require Export ImplemSWp.");
      

      anOut.println("Import Mwp.");

      anOut.incPrintln("Module " + this.getModuleName() + "Annotations.");
      
      for (Method met: methods) {
        final AnnotationMethodExecutor ame = 
            new AnnotationMethodExecutor(this, anOut, fClass, met);
        ame.start();
  
      }
      anOut.decPrintln("End " + this.getModuleName() + "Annotations.\n");
      
    } 
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }




}
