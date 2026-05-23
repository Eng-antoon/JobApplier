package com.aplicator.jobapplier.analytics

object AnalyticsEvents {
    const val APP_OPENED = "app_opened"
    const val SCREEN_VIEWED = "screen_viewed"

    const val SIGNUP_STARTED = "signup_started"
    const val SIGNUP_SUCCEEDED = "signup_succeeded"
    const val SIGNUP_FAILED = "signup_failed"
    const val LOGIN_STARTED = "login_started"
    const val LOGIN_SUCCEEDED = "login_succeeded"
    const val LOGIN_FAILED = "login_failed"
    const val LOGOUT_SUCCEEDED = "logout_succeeded"

    const val ONBOARDING_CHOICE_SELECTED = "onboarding_choice_selected"
    const val ONBOARDING_COMPLETED = "onboarding_completed"

    const val JOB_ADD_STARTED = "job_add_started"
    const val JOB_SOURCE_FETCH_STARTED = "job_source_fetch_started"
    const val JOB_SOURCE_FETCH_SUCCEEDED = "job_source_fetch_succeeded"
    const val JOB_SOURCE_FETCH_FAILED = "job_source_fetch_failed"
    const val JOB_ANALYSIS_STARTED = "job_analysis_started"
    const val JOB_ANALYSIS_SUCCEEDED = "job_analysis_succeeded"
    const val JOB_ANALYSIS_FAILED = "job_analysis_failed"
    const val JOB_DETAIL_VIEWED = "job_detail_viewed"
    const val JOB_STATUS_CHANGED = "job_status_changed"

    const val AI_CONTENT_GENERATION_STARTED = "ai_content_generation_started"
    const val AI_CONTENT_GENERATION_SUCCEEDED = "ai_content_generation_succeeded"
    const val AI_CONTENT_GENERATION_FAILED = "ai_content_generation_failed"
    const val GENERATED_CONTENT_COPIED = "generated_content_copied"
    const val GENERATED_CONTENT_EXPORTED = "generated_content_exported"

    const val RESUME_IMPORT_STARTED = "resume_import_started"
    const val RESUME_PARSE_STARTED = "resume_parse_started"
    const val RESUME_PARSE_SUCCEEDED = "resume_parse_succeeded"
    const val RESUME_PARSE_FAILED = "resume_parse_failed"
    const val RESUME_IMPORT_COMPLETED = "resume_import_completed"
    const val PROFILE_UPDATED = "profile_updated"

    const val SUGGESTIONS_REFRESHED_STARTED = "suggestions_refreshed_started"
    const val SUGGESTIONS_REFRESHED_SUCCEEDED = "suggestions_refreshed_succeeded"
    const val SUGGESTIONS_REFRESHED_FAILED = "suggestions_refreshed_failed"

    const val BUBBLE_PERMISSION_PROMPT_SHOWN = "bubble_permission_prompt_shown"
    const val BUBBLE_PERMISSION_GRANTED = "bubble_permission_granted"
    const val BUBBLE_LAUNCHED = "bubble_launched"
    const val BUBBLE_OPENED = "bubble_opened"
    const val BUBBLE_CLOSED = "bubble_closed"
    const val BUBBLE_DRAG_STARTED = "bubble_drag_started"
    const val BUBBLE_DISMISSED = "bubble_dismissed"
    const val BUBBLE_TAB_SELECTED = "bubble_tab_selected"
    const val BUBBLE_JOB_SELECTED = "bubble_job_selected"
    const val BUBBLE_QUICK_COPY_USED = "bubble_quick_copy_used"
    const val BUBBLE_ACTION_SELECTED = "bubble_action_selected"
    const val BUBBLE_OPEN_APP_CLICKED = "bubble_open_app_clicked"
    const val BUBBLE_ADD_JOB_CLICKED = "bubble_add_job_clicked"
    const val BUBBLE_REFRESH_CLICKED = "bubble_refresh_clicked"

    const val QUOTA_LIMIT_SHOWN = "quota_limit_shown"
    const val QUOTA_EXTRA_REQUESTED = "quota_extra_requested"
    const val QUOTA_EXTRA_REQUEST_SUCCEEDED = "quota_extra_request_succeeded"
    const val QUOTA_EXTRA_REQUEST_FAILED = "quota_extra_request_failed"
}
