package sg.edu.nus.iss.edgp.admin.management.strategy;

import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;


public interface IAPIHelperValidationStrategy<T> {
	
	ValidationResult validateCreation(T data,String header);
	
	ValidationResult validateUpdating(T data, String header);

	ValidationResult validateObject(String data);
	
	ValidationResult validateObject(T data, String header);
	
	ValidationResult validateObjectByUserId(T data,boolean requiresPasswordValidation);
 
}
