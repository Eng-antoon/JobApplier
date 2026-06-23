package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.data.remote.dto.CertificationDto
import com.aplicator.jobapplier.data.remote.dto.EducationDto
import com.aplicator.jobapplier.data.remote.dto.LanguageDto
import com.aplicator.jobapplier.data.remote.dto.ProfileDto
import com.aplicator.jobapplier.data.remote.dto.ProfileUpdateDto
import com.aplicator.jobapplier.data.remote.dto.SkillDto
import com.aplicator.jobapplier.data.remote.dto.WorkExperienceDto
import com.aplicator.jobapplier.domain.model.Certification
import com.aplicator.jobapplier.domain.model.Education
import com.aplicator.jobapplier.domain.model.Language
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProfileRepository"

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : ProfileRepository {

    private val db get() = supabaseClient.postgrest

    override suspend fun getProfile(userId: String): Result<Profile> = runCatching {
        // decodeList().firstOrNull() instead of decodeSingle(): an empty result set
        // (e.g. RLS returns 0 rows during the brief access-token propagation window
        // right after Authenticated) would otherwise throw from decodeSingle and be
        // indistinguishable from a real network/decode error. Treat "no row" as a
        // distinct, typed failure the caller can retry, and never swallow the cause.
        val dto = db.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeList<ProfileDto>().firstOrNull()
            ?: return@runCatching throw NoSuchElementException("profile not found for user $userId")
        dto.toDomain()
    }.onFailure { Log.w(TAG, "getProfile failed for $userId", it) }

    override suspend fun updateProfile(userId: String, profile: Profile): Result<Unit> = runCatching {
        db.from("profiles").update(
            ProfileUpdateDto(
                fullName = profile.fullName,
                email = profile.email,
                phone = profile.phone,
                location = profile.location,
                linkedinUrl = profile.linkedinUrl,
                portfolioUrl = profile.portfolioUrl,
                summary = profile.summary,
                desiredRole = profile.desiredRole,
                desiredSalaryMin = profile.desiredSalaryMin,
                desiredSalaryMax = profile.desiredSalaryMax,
                salaryCurrency = profile.salaryCurrency,
            )
        ) {
            filter { eq("id", userId) }
        }
    }

    override suspend fun markOnboarded(userId: String): Result<Unit> = runCatching {
        db.from("profiles").update(ProfileUpdateDto(isOnboarded = true)) {
            filter { eq("id", userId) }
        }
    }

    // Skills
    override suspend fun getSkills(userId: String): Result<List<Skill>> = runCatching {
        db.from("skills").select {
            filter { eq("user_id", userId) }
        }.decodeList<SkillDto>().map { it.toDomain() }
    }

    override suspend fun upsertSkill(userId: String, skill: Skill): Result<Unit> = runCatching {
        db.from("skills").upsert(
            SkillDto(
                id = skill.id.ifEmpty { null },
                userId = userId,
                name = skill.name,
                category = skill.category,
                proficiency = skill.proficiency,
                yearsExperience = skill.yearsExperience,
            )
        )
    }

    override suspend fun deleteSkill(skillId: String): Result<Unit> = runCatching {
        db.from("skills").delete { filter { eq("id", skillId) } }
    }

    // Work Experiences
    override suspend fun getWorkExperiences(userId: String): Result<List<WorkExperience>> = runCatching {
        db.from("work_experiences").select {
            filter { eq("user_id", userId) }
            order("sort_order", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
        }.decodeList<WorkExperienceDto>().map { it.toDomain() }
    }

    override suspend fun upsertWorkExperience(userId: String, experience: WorkExperience): Result<Unit> = runCatching {
        db.from("work_experiences").upsert(
            WorkExperienceDto(
                id = experience.id.ifEmpty { null },
                userId = userId,
                company = experience.company,
                title = experience.title,
                location = experience.location,
                startDate = experience.startDate ?: "",
                endDate = experience.endDate,
                isCurrent = experience.isCurrent,
                description = experience.description,
                achievements = experience.achievements,
                technologiesUsed = experience.technologiesUsed,
                sortOrder = experience.sortOrder,
            )
        )
    }

    override suspend fun deleteWorkExperience(experienceId: String): Result<Unit> = runCatching {
        db.from("work_experiences").delete { filter { eq("id", experienceId) } }
    }

    // Education
    override suspend fun getEducation(userId: String): Result<List<Education>> = runCatching {
        db.from("education").select {
            filter { eq("user_id", userId) }
        }.decodeList<EducationDto>().map { it.toDomain() }
    }

    override suspend fun upsertEducation(userId: String, education: Education): Result<Unit> = runCatching {
        db.from("education").upsert(
            EducationDto(
                id = education.id.ifEmpty { null },
                userId = userId,
                institution = education.institution,
                degree = education.degree,
                fieldOfStudy = education.fieldOfStudy,
                startDate = education.startDate,
                endDate = education.endDate,
                gpa = education.gpa,
                description = education.description,
            )
        )
    }

    override suspend fun deleteEducation(educationId: String): Result<Unit> = runCatching {
        db.from("education").delete { filter { eq("id", educationId) } }
    }

    // Certifications
    override suspend fun getCertifications(userId: String): Result<List<Certification>> = runCatching {
        db.from("certifications").select {
            filter { eq("user_id", userId) }
        }.decodeList<CertificationDto>().map { it.toDomain() }
    }

    override suspend fun upsertCertification(userId: String, certification: Certification): Result<Unit> = runCatching {
        db.from("certifications").upsert(
            CertificationDto(
                id = certification.id.ifEmpty { null },
                userId = userId,
                name = certification.name,
                issuingOrg = certification.issuingOrg,
                issueDate = certification.issueDate,
                expiryDate = certification.expiryDate,
                credentialUrl = certification.credentialUrl,
            )
        )
    }

    override suspend fun deleteCertification(certificationId: String): Result<Unit> = runCatching {
        db.from("certifications").delete { filter { eq("id", certificationId) } }
    }

    // Languages
    override suspend fun getLanguages(userId: String): Result<List<Language>> = runCatching {
        db.from("languages").select {
            filter { eq("user_id", userId) }
        }.decodeList<LanguageDto>().map { it.toDomain() }
    }

    override suspend fun upsertLanguage(userId: String, language: Language): Result<Unit> = runCatching {
        db.from("languages").upsert(
            LanguageDto(
                id = language.id.ifEmpty { null },
                userId = userId,
                name = language.name,
                proficiency = language.proficiency,
            )
        )
    }

    override suspend fun deleteLanguage(languageId: String): Result<Unit> = runCatching {
        db.from("languages").delete { filter { eq("id", languageId) } }
    }
}

// Mapping extensions
private fun ProfileDto.toDomain() = Profile(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    location = location,
    linkedinUrl = linkedinUrl,
    portfolioUrl = portfolioUrl,
    summary = summary,
    desiredRole = desiredRole,
    desiredSalaryMin = desiredSalaryMin,
    desiredSalaryMax = desiredSalaryMax,
    salaryCurrency = salaryCurrency ?: "USD",
    isOnboarded = isOnboarded,
)

private fun SkillDto.toDomain() = Skill(
    id = id ?: "",
    name = name,
    category = category,
    proficiency = proficiency,
    yearsExperience = yearsExperience,
)

private fun WorkExperienceDto.toDomain() = WorkExperience(
    id = id ?: "",
    company = company,
    title = title,
    location = location,
    startDate = startDate.ifEmpty { null },
    endDate = endDate,
    isCurrent = isCurrent,
    description = description,
    achievements = achievements ?: emptyList(),
    technologiesUsed = technologiesUsed ?: emptyList(),
    sortOrder = sortOrder,
)

private fun EducationDto.toDomain() = Education(
    id = id ?: "",
    institution = institution,
    degree = degree,
    fieldOfStudy = fieldOfStudy,
    startDate = startDate,
    endDate = endDate,
    gpa = gpa,
    description = description,
)

private fun CertificationDto.toDomain() = Certification(
    id = id ?: "",
    name = name,
    issuingOrg = issuingOrg,
    issueDate = issueDate,
    expiryDate = expiryDate,
    credentialUrl = credentialUrl,
)

private fun LanguageDto.toDomain() = Language(
    id = id ?: "",
    name = name,
    proficiency = proficiency,
)
