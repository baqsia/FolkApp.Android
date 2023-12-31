package ge.baqar.gogia.goefolk.storage.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Song")
data class DbSong(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "reference_id") val referenceId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "name_eng") val nameEng: String,
    @ColumnInfo(name = "path") val path: String?,
    @ColumnInfo(name = "ensemble_id") val ensembleId: String,
    @ColumnInfo(name = "song_type") val songType: Int,
    @ColumnInfo(name = "data") var filePath: String,
    @ColumnInfo(name = "is_current") var isCurrentPlaying: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (other is DbSong) {
            return other.referenceId == referenceId
                    && other.name == name
                    && other.nameEng == nameEng
                    && other.ensembleId == ensembleId
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}