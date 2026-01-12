# Git Workflow - Email Notification System

## Branch Strategy

```
main
  └── feature/email-notification
```

## Commit Plan

### Task 1: Project Setup
```
feat(email): add mail and retry dependencies
```

### Task 2: Domain Events
```
feat(email): add rental lifecycle domain events
```

### Task 3: Email Infrastructure
```
feat(email): add email sender abstraction with profile switching
```

### Task 4: Email Templates
```
feat(email): add Thymeleaf templates for notifications
```

### Task 5: Template Service
```
feat(email): implement template rendering service
```

### Task 6: Notification Service
```
feat(email): implement notification service with @Retryable
```

### Task 7: Event Listener
```
feat(email): add async event listeners for notifications
```

### Task 9: RentalService Integration
```
feat(rental): publish events on rental state changes
```

### Task 11: Reminder Scheduler
```
feat(email): add scheduled pickup and return reminders
```

### Task 12: Unit Tests
```
test(email): add unit tests for notification components
```

### Task 14: Integration Tests
```
test(email): add integration tests for event flow
```

### Task 16: Documentation
```
docs(email): document notification system in README
```

## Merge & Rollback

```bash
# Merge
git checkout main
git merge feature/email-notification

# Rollback
git revert <commit-hash>

# Emergency disable
# Set email.enabled=false
```

## Testing

```bash
# Local (MockEmailSender logs emails)
mvn spring-boot:run

# Production
SENDGRID_API_KEY=SG.xxx EMAIL_FROM=noreply@rentacar.com
```
