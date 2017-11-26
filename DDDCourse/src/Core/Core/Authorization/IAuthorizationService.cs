namespace Core.Authorization
{
    public interface IAuthorizationService
    {
        bool CheckPermission(string permission);
    }
}
