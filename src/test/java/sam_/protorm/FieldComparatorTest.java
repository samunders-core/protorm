package sam_.protorm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class FieldComparatorTest {
	@Test(expected=NullPointerException.class)
	public void rejectsNull() {
		Types.ofFields(null);
	}
	
	// given class with 2 violating methods
	private static class With2ViolatingMethods implements LayerDecoder<InputStream> {
		@Field(1)
		public void first() {
			
		}

		@Field(2)
		public Integer second(InputStream inputStream) throws IOException {
			return null;
		}
	}
	
	@Test
	public void rejectsAllConstraintViolatingMethodsAtOnce() {
		try {
			Types.ofFields(With2ViolatingMethods.class);	// when
			Assert.fail("Violations not checked");
		} catch (Error e) {			// then message of both e and its cause contain respective method names
			String combined = e.getMessage() + "\n" + e.getCause().getMessage();	// because no field order is guaranteed
			Assert.assertTrue(combined, combined.contains("public java.lang.Integer " + With2ViolatingMethods.class.getName() + ".second"));
			Assert.assertTrue(combined, combined.contains("public void " + With2ViolatingMethods.class.getName() + ".first"));
		}
	}
	
	// given class defining fields and methods with non-ascending annotation values
	private static class WithReversedAnnotationValues implements LayerDecoder<InputStream> {
		@Field(4) private int f4;
		@Field(3) private int f3;
		@Field(1) private int f1;
		
		@Field(2) private int f2(InputStream inputStream) throws IOException {
			return 0;
		}
	}
	
	@Test
	public void returnsBothFieldsAndMethodsOrderedByValue() {
		SortedMap<AccessibleObject, Class<?>> types = Types.ofFields(WithReversedAnnotationValues.class);	// when
		// then result is not null and keys are in ascending order by Field.value
		Assert.assertNotNull(types);
		Assert.assertTrue(types.toString(), types.keySet().stream().map(java.lang.reflect.Member.class::cast).map(java.lang.reflect.Member::getName).collect(Collectors.toList()).equals(Arrays.asList("f1", "f2", "f3", "f4")));  
	}
}
