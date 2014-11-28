package org.oasis.datacore.core.meta.pov;


public class ProjectException extends RuntimeException {
   private static final long serialVersionUID = 5523029187762475316L;
   private DCProject project = null;
   private String projectName;

   public ProjectException(DCProject project, String message, Throwable cause) {
      super(message, cause);
      this.project = project;
      this.projectName = project.getName();
   }

   public ProjectException(String projectName, String message, Throwable cause) {
      super(message, cause);
      this.projectName = projectName;
   }

   public ProjectException(DCProject project, String message) {
      super(message);
      this.project = project;
      this.projectName = project.getName();
   }

   public ProjectException(String projectName, String message) {
      super(message);
      this.projectName = projectName;
   }

   public String getProjectName() {
      return projectName;
   }

   public DCProject getProject() {
      return project;
   }

}
