package com.aplicator.jobapplier.data.repository

import com.aplicator.jobapplier.domain.model.Certification
import com.aplicator.jobapplier.domain.model.Education
import com.aplicator.jobapplier.domain.model.Language
import com.aplicator.jobapplier.domain.model.Profile
import com.aplicator.jobapplier.domain.model.Skill
import com.aplicator.jobapplier.domain.model.WorkExperience

interface ProfileRepository {
    suspend fun getProfile(userId: String): Result<Profile>
    suspend fun updateProfile(userId: String, profile: Profile): Result<Unit>
    suspend fun markOnboarded(userId: String): Result<Unit>

    suspend fun getSkills(userId: String): Result<List<Skill>>
    suspend fun upsertSkill(userId: String, skill: Skill): Result<Unit>
    suspend fun deleteSkill(skillId: String): Result<Unit>

    suspend fun getWorkExperiences(userId: String): Result<List<WorkExperience>>
    suspend fun upsertWorkExperience(userId: String, experience: WorkExperience): Result<Unit>
    suspend fun deleteWorkExperience(experienceId: String): Result<Unit>

    suspend fun getEducation(userId: String): Result<List<Education>>
    suspend fun upsertEducation(userId: String, education: Education): Result<Unit>
    suspend fun deleteEducation(educationId: String): Result<Unit>

    suspend fun getCertifications(userId: String): Result<List<Certification>>
    suspend fun upsertCertification(userId: String, certification: Certification): Result<Unit>
    suspend fun deleteCertification(certificationId: String): Result<Unit>

    suspend fun getLanguages(userId: String): Result<List<Language>>
    suspend fun upsertLanguage(userId: String, language: Language): Result<Unit>
    suspend fun deleteLanguage(languageId: String): Result<Unit>
}
