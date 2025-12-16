# MinIO Manual CORS Configuration

## Problem with Automated CORS Setup

The MinIO version in use doesn't support CORS configuration via the `mc` command-line tool. However, CORS can be configured manually through the MinIO Console UI.

## Important Note

**Presigned URLs don't require CORS** because authentication is embedded in the URL. The 403 error was likely due to the missing `aqyldy-app` user, which has now been created.

Try uploading again - it should work now without CORS configuration!

## If You Still Need CORS (Optional)

If you encounter CORS errors for direct (non-presigned) requests, configure CORS via the MinIO Console UI:

### Step 1: Access MinIO Console

Open in browser: http://localhost:9001

**Login credentials:**
- Username: `minioadmin`
- Password: `minioadmin123`

### Step 2: Navigate to Bucket Settings

1. Click **Buckets** in the left sidebar
2. Find and click on **aq-media**
3. Click on **Anonymous** tab
4. Click **Add Access Rule**

### Step 3: Add Access Rule

Set the following:
- **Prefix**: `*` (or leave empty for all objects)
- **Access**: `readonly` or `readwrite` (depending on needs)

**Note:** This makes the bucket publicly accessible, which is NOT recommended for production. This is only for development/testing.

### Alternative: Configure via AWS CLI

If you have AWS CLI installed, you can set CORS using S3-compatible API:

```bash
aws --endpoint-url http://localhost:9000 \
    --profile minio \
    s3api put-bucket-cors \
    --bucket aq-media \
    --cors-configuration file://cors-config.xml
```

Where `cors-config.xml` contains:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<CORSConfiguration>
    <CORSRule>
        <AllowedOrigin>http://localhost:4200</AllowedOrigin>
        <AllowedOrigin>http://localhost:8080</AllowedOrigin>
        <AllowedMethod>GET</AllowedMethod>
        <AllowedMethod>PUT</AllowedMethod>
        <AllowedMethod>POST</AllowedMethod>
        <AllowedMethod>DELETE</AllowedMethod>
        <AllowedMethod>HEAD</AllowedMethod>
        <AllowedHeader>*</AllowedHeader>
        <ExposeHeader>ETag</ExposeHeader>
        <MaxAgeSeconds>3600</MaxAgeSeconds>
    </CORSRule>
</CORSConfiguration>
```

## Verification

After configuration, test the upload from your frontend (http://localhost:4200).

### Check Browser DevTools

Open DevTools → Network tab:

**Successful upload shows:**
```
Request URL: http://localhost:9000/aq-media/users/.../photos/file.webp?...
Request Method: PUT
Status Code: 200 OK
```

**With CORS configured, you'll see:**
```
Access-Control-Allow-Origin: http://localhost:4200
```

## Current MinIO Configuration

```
Endpoint:    http://localhost:9000
Console UI:  http://localhost:9001
Bucket:      aq-media (private)
User:        aqyldy-app (enabled, policy attached)
```

## Testing the Upload

Try uploading from your frontend now. The presigned URL upload should work without any CORS configuration because:

1. User `aqyldy-app` exists with proper permissions
2. Bucket `aq-media` is created
3. Policy allows PutObject, GetObject, DeleteObject
4. Presigned URLs bypass CORS restrictions

If it works, you don't need to configure CORS at all!
