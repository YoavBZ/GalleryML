package yoavbz.dupimg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.apache.commons.math3.ml.clustering.Clusterable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import yoavbz.dupimg.background.ImageClassifier;
import yoavbz.dupimg.database.ImageDao;
import yoavbz.dupimg.database.ImageDatabase;
import yoavbz.dupimg.treeview.FileUtils;

@Entity(tableName = "images")
public class Image implements Parcelable, Clusterable {

	public static final Creator<Image> CREATOR = new Creator<Image>() {
		public Image createFromParcel(Parcel in) {
			return new Image(in);
		}

		public Image[] newArray(int size) {
			return new Image[size];
		}
	};

	@Ignore
	@SuppressLint("SimpleDateFormat")
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

	@PrimaryKey
	@NonNull
	private String path;
	private long dateTaken;
	private double[] point;

	public Image() {
	}

	/**
	 * Constructor for Image object:
	 * Extracts EXIF DATETIME attributes, fallbacks to the file lastModified date if unavailable
	 * In addition, generates feature vector
	 *
	 * @param path       The image path
	 * @param context    A context for accessing {@link ImageDatabase}
	 * @param classifier A TensorFlow Lite classifier, for generating feature vector (vector field)
	 */
	public Image(@NonNull String path, @NonNull Context context, @NonNull ImageClassifier classifier) {
		this.path = path;
		point = classifier.recognizeImage(getScaledBitmap());
		getDateTaken(context);
	}

	private Image(@NonNull Parcel in) {
		path = in.readString();
		dateTaken = in.readLong();
		point = in.createDoubleArray();
	}

	public static void delete(String path, Context context) {
		ImageDao dao = ImageDatabase.getAppDatabase(context).imageDao();
		if (FileUtils.deleteFile(context, new File(path))) {
			dao.delete(path);
			Log.d(MainActivity.TAG, "Image: Deleted " + path);
		} else {
			Log.e(MainActivity.TAG, "Image: Couldn't delete path " + path);
		}
	}

	@Nullable
	public Bitmap getOrientedBitmap() {
		try {
			int rotation;
			// Extracting rotation state
			ExifInterface exif = new ExifInterface(path);
			switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotation = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					rotation = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotation = 270;
					break;
				default:
					rotation = ExifInterface.ORIENTATION_UNDEFINED;
			}

			// Constructing a Bitmap
			Bitmap bitmap = BitmapFactory.decodeFile(path);
			Matrix rotationMatrix = new Matrix();
			rotationMatrix.postRotate(rotation);
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, false);
		} catch (Exception e) {
			Log.e(MainActivity.TAG, "Got an exception while constructing oriented Bitmap", e);
		}
		return null;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	public void setPath(@NonNull String path) {
		this.path = path;
	}

	public long getDateTaken() {
		return dateTaken;
	}

	public void setDateTaken(long dateTaken) {
		this.dateTaken = dateTaken;
	}

	public long getDateTaken(Context context) {
		if (dateTaken == 0) {
			try (InputStream in = new FileInputStream(path)) {
				// Extracting DATETIME_ORIGINAL
				ExifInterface exif = new ExifInterface(in);
				String date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
				dateTaken = dateFormat.parse(date).getTime();
			} catch (Exception e) {
				Log.e(MainActivity.TAG, "Image - Got an exception while extracting date from " + path, e);
				// Fallback - Extracting creationTime attribute
				dateTaken = new File(path).lastModified();
			}
			// Update image in DB
			ImageDatabase.getAppDatabase(context).imageDao().update(this);
		}
		return dateTaken;
	}

	@NonNull
	@Override
	public String toString() {
		return path;
	}

	/**
	 * @return A scaled Bitmap representation of the image (224x224 pixels)
	 */
	private Bitmap getScaledBitmap() {
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		return Bitmap.createScaledBitmap(bitmap, 224, 224, false);
	}

	@Override
	public double[] getPoint() {
		return point;
	}

	public void setPoint(double[] point) {
		this.point = point;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(path);
		dest.writeLong(dateTaken);
		dest.writeDoubleArray(point);
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
