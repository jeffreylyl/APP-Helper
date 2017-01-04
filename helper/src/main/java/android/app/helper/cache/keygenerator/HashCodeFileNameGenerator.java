package android.app.helper.cache.keygenerator;

public class HashCodeFileNameGenerator implements IKeyGenerator {
	@Override
	public String generate(String imageUri) {
		return String.valueOf(imageUri.hashCode());
	}
}