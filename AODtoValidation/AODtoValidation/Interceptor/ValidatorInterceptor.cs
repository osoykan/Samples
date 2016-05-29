namespace AODtoValidation.Interceptor
{
    #region using

    using System;
    using System.Collections.Generic;
    using System.Collections.ObjectModel;
    using System.Linq;
    using System.Reflection;
    using Castle.DynamicProxy;
    using Castle.MicroKernel;
    using FluentValidation;
    using FluentValidation.Internal;
    using FluentValidation.Results;
    using Service;

    #endregion

    public class ValidatorInterceptor : IInterceptor
    {
        public ValidatorInterceptor(IKernel kernel)
        {
            _kernel = kernel;
        }

        private IKernel _kernel { get; }

        public void Intercept(IInvocation invocation)
        {
            AssertRequest(invocation);
            invocation.Proceed();
        }

        private static string[] GetOrDefaultValidatorRuleSets(MethodInfo method)
        {
            var rules = new List<string> {"default"};
            var attribute = method.CustomAttributes.FirstOrDefault(x => x.AttributeType == typeof (ValidateWithRuleAttribute));
            if (attribute == null)
                return rules.ToArray();

            rules.AddRange((attribute.ConstructorArguments.First().Value as ReadOnlyCollection<CustomAttributeTypedArgument>)
                .Select(x => x.Value.ToString())
                .ToList());

            return rules.ToArray();
        }

        private static ValidationResult ValidateTyped<T>(IValidator<T> validator, T request, string[] ruleset, IValidatorSelector selector = null)
        {
            return validator.Validate(request, selector, string.Join(",", ruleset).TrimEnd(','));
        }

        private void AssertRequest(IInvocation invocation)
        {
            var requestObject = invocation.Arguments[0];
            var ruleSets = GetOrDefaultValidatorRuleSets(invocation.Method);
            var requestValidatorType = typeof (IValidator<>).MakeGenericType(requestObject.GetType());

            var validator = _kernel.Resolve(requestValidatorType);

            if (validator == null)
                return;

            var validationResult = GetType()
                .GetMethod("ValidateTyped", BindingFlags.Static | BindingFlags.NonPublic)
                .MakeGenericMethod(requestObject.GetType())
                .Invoke(null, new[] {validator, requestObject, ruleSets, null}) as ValidationResult;

            _kernel.ReleaseComponent(validator);

            if (validationResult != null && validationResult.IsValid)
                return;

            if (validationResult != null && validationResult.Errors.Any())
                throw new InvalidOperationException(string.Join(",", validationResult.Errors.Select(x => x.ErrorMessage)));
        }
    }
}