namespace Core.Session
{
    public interface ISession
    {
        string Username { get; }

        string Email { get; }

        string UserId { get; }
    }
}
