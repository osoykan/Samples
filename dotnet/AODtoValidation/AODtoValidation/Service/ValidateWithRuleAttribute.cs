namespace AODtoValidation.Service
{
    #region using

    using System;

    #endregion

    [Serializable]
    public class ValidateWithRuleAttribute : Attribute
    {
        public ValidateWithRuleAttribute(params string[] ruleSets)
        {
            RuleSetNames = ruleSets;
        }

        private string[] RuleSetNames { get; set; }
    }
}