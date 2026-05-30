# ImgBB API Setup Guide

## How to Get Your ImgBB API Key

1. **Visit ImgBB**
   - Go to https://api.imgbb.com/ or https://imgbb.com/

2. **Create an Account (if needed)**
   - Sign up for a free account at https://imgbb.com/signup
   - Verify your email address

3. **Get Your API Key**
   - After logging in, go to https://api.imgbb.com/
   - You'll see your API key displayed on the page
   - Or check your account settings/dashboard for the API section

4. **Add API Key to Your Project**
   - Open `app/src/main/java/com/example/healthconnect/data/ImageUploadService.kt`
   - Find the line: `private val apiKey = "YOUR_IMGBB_API_KEY"`
   - Replace `"YOUR_IMGBB_API_KEY"` with your actual API key from ImgBB
   - Example: `private val apiKey = "abc123def456ghi789"`

## ImgBB API Limits

- **Free Tier**: 32MB per image, unlimited uploads
- **Rate Limits**: Varies by plan
- **Storage**: Images are stored permanently unless deleted

## Testing

After adding your API key:
1. Try uploading a doctor image in the admin panel
2. Check Logcat for any errors (tag: "ImageUploadService")
3. Verify the image URL is saved in Firestore

## Security Note

⚠️ **Important**: For production apps, consider storing the API key securely:
- Use Android's `BuildConfig` or `local.properties`
- Or use environment variables
- Never commit API keys to public repositories

