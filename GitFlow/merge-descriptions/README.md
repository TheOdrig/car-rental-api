# Merge Description Templates

Bu klasör, GitHub/GitLab PR (Pull Request) ve MR (Merge Request) açıklamaları için profesyonel template'ler içerir.

## 📁 Dosyalar

| Dosya | Kullanım | Ne Zaman? |
|-------|----------|-----------|
| [FEATURE_TO_DEVELOP.md](./FEATURE_TO_DEVELOP.md) | Feature branch → develop | Yeni özellik geliştirme tamamlandığında |
| [DEVELOP_TO_MAIN.md](./DEVELOP_TO_MAIN.md) | develop → main | Production deployment yapılacağında |

## 🚀 Hızlı Kullanım

### 1. Feature → Develop
```bash
# Branch: feature/damage-management → develop
# PR Title: feat(damage): implement damage management system
```
→ `FEATURE_TO_DEVELOP.md` template'ini kullan

### 2. Develop → Main
```bash
# Branch: develop → main
# PR Title: release: damage management system to production
```
→ `DEVELOP_TO_MAIN.md` template'ini kullan

## 📝 Conventional Commits Cheat Sheet

| Type | Açıklama | Örnek |
|------|----------|-------|
| `feat` | Yeni özellik | `feat(auth): add OAuth2 login` |
| `fix` | Bug düzeltme | `fix(rental): resolve date overlap` |
| `refactor` | Kod iyileştirme | `refactor(payment): simplify flow` |
| `docs` | Dokümantasyon | `docs(readme): update API section` |
| `test` | Test ekleme | `test(damage): add E2E tests` |
| `chore` | Build/config | `chore(deps): update Spring Boot` |
| `release` | Production release | `release: v1.2.0` |

## ✅ PR Checklist

Her PR'da kontrol et:

- [ ] Title follows Conventional Commits format
- [ ] Description filled with template
- [ ] All tests passing
- [ ] No merge conflicts
- [ ] Documentation updated
- [ ] Breaking changes documented (if any)

## 🎯 İyi PR Açıklaması Özellikleri

1. **Clear Summary** - 1-2 cümle, ne yapıldığını anlatır
2. **Context** - Neden bu değişiklik gerekti?
3. **Changes List** - Bullet points ile değişiklikler
4. **Testing Evidence** - Test coverage ve sonuçlar
5. **Breaking Changes** - Geriye uyumluluk etkileri
6. **Deployment Notes** - (Main'e merge için) Deployment adımları

## 🔗 İlgili Kaynaklar

- [Conventional Commits](https://www.conventionalcommits.org/)
- [How to Write Good Commit Messages](https://cbea.ms/git-commit/)
- [GitHub PR Best Practices](https://docs.github.com/en/pull-requests)
