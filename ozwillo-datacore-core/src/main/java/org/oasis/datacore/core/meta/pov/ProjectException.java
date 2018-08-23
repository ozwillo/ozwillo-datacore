package org.oasis.datacore.core.meta.pov;



public class ProjectException extends RuntimeException {
   private static final long serialVersionUID = 5523029187762475316L;
   private DCProject project = null;
   private String projectName;

   public ProjectException(DCProject project, String message) {
      super(buildMessageFromModelMessage(message, project, null));
      this.project = project;
      this.projectName = project.getName();
   }

   public ProjectException(String projectName, String message) {
      super(buildMessageFromModelMessage(message, null, projectName));
      this.projectName = projectName;
   }

   public String getProjectName() {
      return projectName;
   }

   public DCProject getProject() {
      return project;
   }

   private static String buildMessageFromModelMessage(
         String modelMessage, DCProject project, String projectName) {
      StringBuilder sb;
      if (project == null && projectName == null) {
         sb = new StringBuilder();
      } else {
         sb = new StringBuilder("On project ");
         if (project != null) {
            sb.append(project.getName());
         } else {
            sb.append(projectName);
         }
         sb.append(" ");
      }
      sb.append(": ");
      sb.append(modelMessage);
      return sb.toString();
   }

}
