# Contributing to Pascal Scanning Tool

Thank you for your interest in contributing to Pascal Scanning Tool! This document provides guidelines and information for contributors.

## 🚀 **Getting Started**

### **Prerequisites**

- **Java**: JDK 17 or higher
- **Maven**: 3.8 or higher
- **Git**: For version control
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code (recommended)

### **Development Setup**

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/pascal-scanning-tool.git
   cd pascal-scanning-tool
   ```
3. **Add upstream** remote:
   ```bash
   git remote add upstream https://github.com/pascagihozo/pascal-scanning-tool.git
   ```
4. **Create** a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 📝 **Commit Guidelines**

### **Commit Message Format**

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### **Types**

- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to the build process or auxiliary tools

### **Examples**

```bash
feat(api): add scanner discovery endpoint
fix(wia): resolve Windows scanner detection issue
docs: update installation instructions
refactor(escl): improve network scanner handling
test(api): add unit tests for scanner controller
```

## 🔧 **Development Workflow**

### **Making Changes**

1. **Create** a feature branch from `main`
2. **Make** your changes
3. **Test** your changes thoroughly
4. **Commit** with clear messages
5. **Push** to your fork
6. **Create** a Pull Request

### **Testing**

- **Unit Tests**: Write tests for new functionality
- **Integration Tests**: Test scanner integration
- **Cross-Platform**: Test on Windows, macOS, and Linux
- **Manual Testing**: Test with real scanners

### **Code Style**

- Follow existing code patterns
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and small
- Use proper exception handling

## 🐛 **Reporting Issues**

### **Bug Reports**

When reporting bugs, please include:

1. **Description**: Clear description of the issue
2. **Steps to Reproduce**: Detailed steps to reproduce
3. **Expected Behavior**: What should happen
4. **Actual Behavior**: What actually happens
5. **Environment**: OS, Java version, scanner model
6. **Logs**: Relevant log files or error messages

### **Feature Requests**

When requesting features, please include:

1. **Description**: Clear description of the feature
2. **Use Case**: Why this feature would be useful
3. **Proposed Solution**: How you think it should work
4. **Alternatives**: Other solutions you've considered

## 🔄 **Pull Request Process**

### **Before Submitting**

- [ ] Code follows the project's style guidelines
- [ ] Self-review of your code
- [ ] Tests pass locally
- [ ] Documentation updated if needed
- [ ] Commit messages follow conventional format

### **Pull Request Template**

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Cross-platform testing (if applicable)

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or documented)
```

## 🏗️ **Architecture Guidelines**

### **Scanner Backend Integration**

When adding new scanner backends:

1. **Create** a service class in appropriate package (`wia/`, `sane/`, `escl/`)
2. **Implement** common scanner interface
3. **Add** configuration properties
4. **Update** scanner facade to include new backend
5. **Add** tests for the new backend

### **API Design**

- Follow RESTful principles
- Use appropriate HTTP methods
- Return consistent response formats
- Handle errors gracefully
- Document API endpoints

### **Configuration**

- Use Spring Boot configuration properties
- Provide sensible defaults
- Document all configuration options
- Validate configuration on startup

## 🧪 **Testing Guidelines**

### **Unit Tests**

- Test individual methods and classes
- Mock external dependencies
- Aim for high code coverage
- Test edge cases and error conditions

### **Integration Tests**

- Test scanner backend integration
- Test API endpoints
- Test configuration loading
- Test cross-platform compatibility

### **Manual Testing**

- Test with real scanners
- Test on different operating systems
- Test with different scanner models
- Test error scenarios

## 📚 **Documentation**

### **Code Documentation**

- Add JavaDoc comments for public APIs
- Include parameter descriptions
- Document return values
- Add usage examples

### **User Documentation**

- Update README.md for user-facing changes
- Add installation instructions for new platforms
- Document new configuration options
- Update troubleshooting guides

## 🚀 **Release Process**

### **Version Numbering**

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality in a backwards compatible manner
- **PATCH**: Backwards compatible bug fixes

### **Release Checklist**

- [ ] All tests pass
- [ ] Documentation updated
- [ ] Version numbers updated
- [ ] CHANGELOG.md updated
- [ ] Release notes prepared
- [ ] Cross-platform builds tested

## 💬 **Communication**

### **Getting Help**

- **GitHub Issues**: For bug reports and feature requests
- **GitHub Discussions**: For general questions and discussions
- **Email**: pascagihozo@gmail.com for private matters

### **Code of Conduct**

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Follow the golden rule

## 🏆 **Recognition**

Contributors will be recognized in:

- **README.md**: Listed as contributors
- **CHANGELOG.md**: Mentioned in release notes
- **GitHub**: Listed in contributors section

Thank you for contributing to Pascal Scanning Tool! 🎉

---

**Questions?** Feel free to reach out via GitHub Issues or email!
