namespace AODtoValidation.Validators
{
    #region using

    using System;
    using Dtos;
    using FluentValidation;

    #endregion

    public class ValidatorBase<T> : AbstractValidator<T> where T : RequestBase
    {
        public ValidatorBase()
        {
            RuleFor(x => x.LanguageCode)
                .NotNull().WithMessage("LanguageCode cannot be null")
                .NotEmpty().WithMessage("LanguageCode cannot be empty or null");

            RuleFor(x => x.TerminalCode)
                .NotNull().WithMessage("TerminalCode cannot be null")
                .NotEmpty().WithMessage("TerminalCode cannot be empty or null");

            RuleFor(x => x.TerminalSerialNumber)
                .NotNull().WithMessage("TerminalSerialNumber cannot be null")
                .NotEmpty().WithMessage("TerminalSerialNumber cannot be empty or null");

            RuleFor(x => x.TrackId)
                .NotNull().WithMessage("TrackId cannot be null")
                .Must(x => x != Guid.Empty).WithMessage("TrackId cannot be empty GUID!");

            RuleFor(x => x.TransactionDateTime).Must(t =>
            {
                DateTime dateTime;
                return DateTime.TryParse(t, out dateTime);
            }).WithMessage("Please fill CreateDateTime correctly!");
        }
    }
}